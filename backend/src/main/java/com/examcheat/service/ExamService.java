package com.examcheat.service;

import com.examcheat.dto.Dtos.*;
import com.examcheat.entity.*;
import com.examcheat.event.*;
import com.examcheat.exception.ApiException;
import com.examcheat.exception.InvalidChoiceException;
import com.examcheat.model.*;
import com.examcheat.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * All exam business rules. The backend is the source of truth: scores, risk, marks and reports are
 * always computed here from saved rows, through the OOP domain classes.
 *
 * DEMO AUTHENTICATION: start() returns a random session token; every later call must send it as the
 * X-Session-Token header. This stops one student's browser touching another student's session. It does
 * NOT verify who the student really is (no password / SSO) - see README "Security".
 */
@Service
@Transactional
public class ExamService {
    private static final Pattern EVENT_ID = Pattern.compile("^[A-Za-z0-9:_-]{8,64}$");

    private final StudentRepository students;
    private final ExamSessionRepository sessions;
    private final AnswerRepository answers;
    private final SuspiciousEventRepository events;
    private final ReportService reports;
    private final EventRateLimiter limiter;
    private final Clock clock;
    private final int activeSeconds;
    private final PolicyView policy;
    private final SecureRandom random = new SecureRandom();

    public ExamService(StudentRepository students, ExamSessionRepository sessions, AnswerRepository answers,
                       SuspiciousEventRepository events, ReportService reports, EventRateLimiter limiter, Clock clock,
                       @Value("${app.session-active-seconds:120}") int activeSeconds,
                       @Value("${exam.policy.block-clipboard:true}") boolean blockClipboard,
                       @Value("${exam.policy.block-context-menu:true}") boolean blockContextMenu) {
        this.students = students;
        this.sessions = sessions;
        this.answers = answers;
        this.events = events;
        this.reports = reports;
        this.limiter = limiter;
        this.clock = clock;
        this.activeSeconds = activeSeconds;
        this.policy = new PolicyView(blockClipboard, blockContextMenu);
    }

    // =====================================================================================
    // Registration and start
    // =====================================================================================

    public StudentView register(RegisterRequest req) {
        String name = req.name().trim();
        if (name.isEmpty()) {
            throw new InvalidChoiceException("Student name is required.");
        }
        StudentEntity s = students.findById(req.studentId()).orElse(null);
        if (s == null) {
            s = new StudentEntity(req.studentId(), name, clock.instant());
        } else {
            s.setName(name);
        }
        s = students.save(s);
        return new StudentView(s.getStudentId(), s.getName(), s.getRegisteredAt().toString());
    }

    /**
     * MULTIPLE-LOGIN RULE (deterministic):
     *  1. If this student already has an unfinished session started by the SAME clientId (same browser tab,
     *     e.g. a page refresh) -> the session is resumed, NO event is recorded.
     *  2. Otherwise, if the student has unfinished sessions that were used within the last
     *     app.session-active-seconds seconds (started by a DIFFERENT clientId) -> a MULTIPLE_LOGIN event is
     *     recorded on each of them AND on the new session. The event id is "multi-login:" + clientId, so
     *     repeating the same request never scores twice.
     *  3. Abandoned (stale) sessions never trigger the rule.
     */
    public StartResponse start(StartRequest req) {
        StudentEntity student = students.findById(req.studentId())
                .orElseThrow(() -> new ApiException(404, "Student " + req.studentId() + " is not registered. Register first."));
        TestPaper paper = PaperBank.getPaper(req.paperId());   // InvalidChoiceException -> 400
        Instant now = clock.instant();
        String token = newToken();

        List<ExamSessionEntity> open = sessions.findByStudentIdAndStatusOrderByStartedAtAsc(student.getStudentId(), SessionStatus.IN_PROGRESS);

        // Rule 1: resume (refresh)
        for (int i = open.size() - 1; i >= 0; i--) {
            ExamSessionEntity s = open.get(i);
            if (s.getClientId().equals(req.clientId())) {
                s.setTokenHash(hash(token));
                s.setLastSeenAt(now);
                sessions.save(s);
                return new StartResponse(s.getId(), token, true, false, buildState(s));
            }
        }

        // Rule 2: another client is actively taking the exam
        String loginEventId = "multi-login:" + req.clientId();
        boolean multiple = false;
        for (ExamSessionEntity old : open) {
            if (now.minusSeconds(activeSeconds).isBefore(old.getLastSeenAt())) {
                multiple = true;
                if (events.findBySessionIdAndEventId(old.getId(), loginEventId).isEmpty()) {
                    appendEvent(old.getId(), loginEventId, EventType.MULTIPLE_LOGIN, now, EventSource.SERVER_RULE,
                            "Second login for student " + student.getStudentId(), now);
                }
            }
        }

        ExamSessionEntity created = new ExamSessionEntity(UUID.randomUUID().toString(), student.getStudentId(),
                student.getName(), paper.getId(), req.clientId(), hash(token), now);
        created = sessions.save(created);
        if (multiple) {
            appendEvent(created.getId(), loginEventId, EventType.MULTIPLE_LOGIN, now, EventSource.SERVER_RULE,
                    "Started while another session was active", now);
        }
        return new StartResponse(created.getId(), token, false, multiple, buildState(created));
    }

    // =====================================================================================
    // Reading state / questions
    // =====================================================================================

    public SessionState getState(String sessionId, String token) {
        return buildState(authorize(sessionId, token));
    }

    public QuestionView getQuestion(String sessionId, String token, int number) {
        ExamSessionEntity s = authorize(sessionId, token);
        TestPaper paper = PaperBank.getPaper(s.getPaperId());
        if (number < 1 || number > paper.getQuestionCount()) {
            throw new InvalidChoiceException("Question number must be between 1 and " + paper.getQuestionCount() + ".");
        }
        String selected = null;
        for (AnswerEntity a : answers.findBySessionIdOrderByQuestionNumberAsc(sessionId)) {
            if (a.getQuestionNumber() == number) {
                selected = a.getSelectedOption();
            }
        }
        return PaperService.questionView(paper, number, selected);
    }

    // =====================================================================================
    // Events (idempotent)
    // =====================================================================================

    public EventResult recordEvent(String sessionId, String token, EventRequest req) {
        ExamSessionEntity s = authorize(sessionId, token);
        requireInProgress(s);
        if (!limiter.tryAcquire(sessionId)) {
            throw new ApiException(429, "Too many events for this session. Slow down.");
        }
        if (!EVENT_ID.matcher(req.eventId()).matches()) {
            throw new InvalidChoiceException("eventId must be 8-64 characters: letters, digits, ':', '_' or '-'.");
        }
        EventType type = EventType.parse(req.type());
        EventSource source = parseClientSource(req.source());
        Instant at = parseTimestamp(req.timestamp(), s);

        // Idempotency: an event id that was already stored is acknowledged but NOT scored again.
        SuspiciousEventEntity existing = events.findBySessionIdAndEventId(sessionId, req.eventId()).orElse(null);
        boolean duplicate = existing != null;
        if (!duplicate) {
            appendEvent(sessionId, req.eventId(), type, at, source, req.details(), clock.instant());
        }
        ExamMonitor m = buildMonitor(s);
        CheatEvent shown = null;
        for (CheatEvent e : m.getEvents()) {
            if (req.eventId().equals(e.getEventId())) {
                shown = e;
            }
        }
        return new EventResult(duplicate, m.calculateScore(), m.classifyRisk(), m.getEvents().size(), shown == null ? null : view(shown));
    }

    // =====================================================================================
    // Answers
    // =====================================================================================

    public AnswerResult answer(String sessionId, String token, AnswerRequest req) {
        ExamSessionEntity s = authorize(sessionId, token);
        requireInProgress(s);
        TestPaper paper = PaperBank.getPaper(s.getPaperId());
        int number = req.questionNumber();
        if (number < 1 || number > paper.getQuestionCount()) {
            throw new InvalidChoiceException("Question number must be between 1 and " + paper.getQuestionCount() + ".");
        }
        String option = req.selectedOption().trim().toUpperCase();
        if (option.length() != 1 || option.charAt(0) < 'A' || option.charAt(0) > 'D') {
            throw new InvalidChoiceException("Answer must be A, B, C or D.");
        }
        if (answers.existsBySessionIdAndQuestionNumber(sessionId, number)) {
            throw new ApiException(409, "Question " + number + " has already been answered.");
        }
        // marking happens here, in the domain class; nothing about correctness is taken from the browser
        Question q = paper.getQuestion(number - 1);
        boolean correct = q.isCorrect(option.charAt(0));
        answers.save(new AnswerEntity(sessionId, number, option, correct, correct ? q.getMarks() : 0, clock.instant()));
        int attempted = answers.findBySessionIdOrderByQuestionNumberAsc(sessionId).size();
        return new AnswerResult(number, attempted, paper.getQuestionCount());
    }

    // =====================================================================================
    // Finish and reports
    // =====================================================================================

    public FinishResult finish(String sessionId, String token) {
        ExamSessionEntity s = authorize(sessionId, token);
        if (s.getStatus() == SessionStatus.IN_PROGRESS) {
            s.setStatus(SessionStatus.COMPLETED);
            s.setFinishedAt(clock.instant());
            s = sessions.save(s);
        }
        ExamMonitor m = buildMonitor(s);
        reports.save(s, m);
        List<ReviewItem> review = new ArrayList<>();
        TestPaper paper = m.getPaper();
        for (int i = 1; i <= paper.getQuestionCount(); i++) {
            Question q = paper.getQuestion(i - 1);
            Character chosen = m.getAnswers().get(i);
            boolean ok = chosen != null && q.isCorrect(chosen);
            review.add(new ReviewItem(i, q.getText(), java.util.Arrays.asList(q.getOptions()),
                    chosen == null ? null : String.valueOf(chosen), String.valueOf(q.getCorrectOption()), ok, ok ? q.getMarks() : 0));
        }
        return new FinishResult(s.getId(), m.getMarksObtained(), paper.getTotalMarks(), m.getQuestionsAttempted(),
                paper.getQuestionCount(), m.calculateScore(), m.classifyRisk(), reports.fileName(s), review, buildState(s));
    }

    public ReportResult report(String sessionId, String token) {
        ExamSessionEntity s = authorize(sessionId, token);
        requireCompleted(s);
        String text = reports.save(s, buildMonitor(s));
        return new ReportResult(s.getId(), reports.fileName(s), text);
    }

    /** Returns {fileName, text}. */
    public String[] reportForDownload(String sessionId, String token) {
        ReportResult r = report(sessionId, token);
        return new String[]{r.fileName(), r.report()};
    }

    // =====================================================================================
    // helpers
    // =====================================================================================

    private ExamSessionEntity authorize(String sessionId, String token) {
        ExamSessionEntity s = sessions.findById(sessionId)
                .orElseThrow(() -> new ApiException(404, "Exam session not found."));
        if (token == null || token.isBlank()) {
            throw new ApiException(401, "Missing X-Session-Token header.");
        }
        if (!MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.UTF_8), s.getTokenHash().getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(403, "This session token does not belong to that exam session.");
        }
        s.setLastSeenAt(clock.instant());
        return sessions.save(s);
    }

    private void requireInProgress(ExamSessionEntity s) {
        if (s.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new ApiException(409, "This exam has already been submitted.");
        }
    }

    private void requireCompleted(ExamSessionEntity s) {
        if (s.getStatus() != SessionStatus.COMPLETED) {
            throw new ApiException(409, "Finish the exam before generating the report.");
        }
    }

    /** Clients may only claim BROWSER_EXTENSION or SIMULATED_DEMO; SERVER_RULE is reserved for the server. */
    private EventSource parseClientSource(String text) {
        if (text == null || text.isBlank()) {
            return EventSource.BROWSER_EXTENSION;
        }
        try {
            EventSource src = EventSource.valueOf(text.trim().toUpperCase());
            if (src == EventSource.SERVER_RULE) {
                throw new InvalidChoiceException("source SERVER_RULE is reserved for the server.");
            }
            return src;
        } catch (IllegalArgumentException e) {
            throw new InvalidChoiceException("Unknown event source: " + text);
        }
    }

    private Instant parseTimestamp(String text, ExamSessionEntity s) {
        Instant at;
        try {
            at = Instant.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidChoiceException("timestamp must be an ISO-8601 instant, e.g. 2026-10-10T10:15:30Z.");
        }
        Instant now = clock.instant();
        if (at.isAfter(now.plusSeconds(120))) {
            throw new InvalidChoiceException("timestamp is in the future.");
        }
        if (at.isBefore(s.getStartedAt().minusSeconds(60))) {
            throw new InvalidChoiceException("timestamp is before the exam started.");
        }
        return at;
    }

    private void appendEvent(String sessionId, String eventId, EventType type, Instant at, EventSource source,
                             String details, Instant receivedAt) {
        // The points come from the polymorphic domain object - never from the request.
        CheatEvent domainEvent = CheatEventFactory.create(type, at, source, eventId);
        events.save(new SuspiciousEventEntity(sessionId, eventId, type.name(), at, receivedAt,
                domainEvent.calculateScore(), source.name(), details));
    }

    /** Rebuilds the OOP domain object graph from the saved rows. */
    private ExamMonitor buildMonitor(ExamSessionEntity s) {
        TestPaper paper = PaperBank.getPaper(s.getPaperId());
        ExamMonitor m = new ExamMonitor(new Student(s.getStudentId(), s.getStudentName(), paper.getPaperName()), paper);
        for (SuspiciousEventEntity e : events.findBySessionIdOrderByOccurredAtAscIdAsc(s.getId())) {
            m.recordEvent(CheatEventFactory.create(EventType.valueOf(e.getEventType()), e.getOccurredAt(),
                    EventSource.valueOf(e.getEventSource()), e.getEventId()));
        }
        for (AnswerEntity a : answers.findBySessionIdOrderByQuestionNumberAsc(s.getId())) {
            m.submitAnswer(a.getQuestionNumber(), a.getSelectedOption().charAt(0));
        }
        return m;
    }

    private EventView view(CheatEvent e) {
        return new EventView(e.getEventId(), e.getType().name(), e.getEventName(), e.getOccurredAt().toString(),
                e.calculateScore(), e.getSource().name());
    }

    private SessionState buildState(ExamSessionEntity s) {
        ExamMonitor m = buildMonitor(s);
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        for (EventType t : EventType.values()) {
            breakdown.put(t.name(), 0);
        }
        List<EventView> list = new ArrayList<>();
        for (CheatEvent e : m.getEvents()) {
            breakdown.merge(e.getType().name(), 1, Integer::sum);
            list.add(view(e));
        }
        Map<Integer, String> chosen = new LinkedHashMap<>();
        for (Map.Entry<Integer, Character> a : m.getAnswers().entrySet()) {
            chosen.put(a.getKey(), String.valueOf(a.getValue()));   // option only - no correctness
        }
        boolean done = s.getStatus() == SessionStatus.COMPLETED;
        return new SessionState(s.getId(), s.getStudentId(), s.getStudentName(), s.getPaperId(), m.getPaper().getPaperName(),
                s.getStatus().name(), s.getStartedAt().toString(), s.getFinishedAt() == null ? null : s.getFinishedAt().toString(),
                m.getPaper().getQuestionCount(), m.getQuestionsAttempted(), chosen,
                m.calculateScore(), m.classifyRisk(), list.size(), breakdown, list,
                m.getPaper().getTotalMarks(), done ? m.getMarksObtained() : null, policy);
    }

    private String newToken() {
        byte[] b = new byte[24];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
