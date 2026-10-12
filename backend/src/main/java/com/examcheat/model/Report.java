package com.examcheat.model;

import com.examcheat.event.CheatEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Builds the report text from the ExamMonitor and saves it to a file (FILE HANDLING).
 * One report per session, generated only from saved data, so the output is deterministic.
 */
public class Report {
    private final ExamMonitor monitor;
    private final String sessionId;
    private final Instant startedAt;
    private final Instant finishedAt;

    public Report(ExamMonitor monitor, String sessionId, Instant startedAt, Instant finishedAt) {
        this.monitor = monitor;
        this.sessionId = sessionId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public String generateReport() {
        Student s = monitor.getStudent();
        TestPaper p = monitor.getPaper();
        String r = "===== EXAM CHEAT DETECTION REPORT =====\n"
                + "Session ID          : " + sessionId + "\n"
                + "Student ID          : " + s.getStudentId() + "\n"
                + "Student Name        : " + s.getName() + "\n"
                + "Exam                : " + s.getExamName() + "\n"
                + "Session Started     : " + CheatEvent.format(startedAt) + "\n"
                + "Session Completed   : " + (finishedAt == null ? "not completed" : CheatEvent.format(finishedAt)) + "\n"
                + "Questions Attempted : " + monitor.getQuestionsAttempted() + " / " + p.getQuestionCount() + "\n"
                + "Marks Obtained      : " + monitor.getMarksObtained() + " / " + p.getTotalMarks() + "\n"
                + "Tab Switches        : " + monitor.countEvents("Tab switch") + "\n"
                + "Copy/Paste Attempts : " + monitor.countEvents("Copy/Paste attempt") + "\n"
                + "Multiple Logins     : " + monitor.countEvents("Multiple login") + "\n"
                + "Window Exits        : " + monitor.countEvents("Exam window exit") + "\n"
                + "Person Detection    : " + monitor.countEvents("Multiple person") + " (simulated in this prototype)\n"
                + "Cheat Score         : " + monitor.calculateScore() + "\n"
                + "Risk Level          : " + monitor.classifyRisk() + "\n";

        r = r + "----------- Event Log (chronological) -----------\n";
        List<CheatEvent> list = monitor.getEvents();
        if (list.size() == 0) {
            r = r + "No suspicious events recorded.\n";
        } else {
            for (int i = 0; i < list.size(); i++) {
                CheatEvent e = list.get(i);
                r = r + (i + 1) + ". " + e.getEventName() + " at " + e.getTimestamp()
                        + " (+" + e.calculateScore() + ") [" + e.getSource().getLabel() + "]\n";
            }
        }
        r = r + "-------------------------------------------------\n"
                + "Note: browser signals are limited and do not prove cheating. They are indicators for review.\n"
                + "=================================================";
        return r;
    }

    /** File handling: writes (overwrites) one file for this session and returns its path. */
    public Path saveToFile(Path directory, String fileName) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(fileName);
        try (FileWriter writer = new FileWriter(file.toFile(), false)) {
            writer.write(generateReport() + "\n");
        }
        return file;
    }
}
