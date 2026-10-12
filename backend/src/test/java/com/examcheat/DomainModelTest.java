package com.examcheat;

import com.examcheat.event.*;
import com.examcheat.exception.InvalidChoiceException;
import com.examcheat.model.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests of the OOP domain classes (no Spring, no database). */
class DomainModelTest {
    private static final Instant T = Instant.parse("2026-10-10T10:00:00Z");

    private ExamMonitor monitor(int paperId) {
        TestPaper p = PaperBank.getPaper(paperId);
        return new ExamMonitor(new Student(1013, "Shreya", p.getPaperName()), p);
    }

    private CheatEvent ev(EventType t) {
        return CheatEventFactory.create(t, T, EventSource.BROWSER_EXTENSION, "id-" + t + "-xxxx");
    }

    @Test
    void eachEventHasTheScoreFromTheProjectDocument() {
        assertEquals(2, ev(EventType.TAB_SWITCH).calculateScore());
        assertEquals(3, ev(EventType.COPY_PASTE).calculateScore());
        assertEquals(4, ev(EventType.MULTIPLE_LOGIN).calculateScore());
        assertEquals(3, ev(EventType.WINDOW_EXIT).calculateScore());
        assertEquals(5, ev(EventType.MULTIPLE_PERSON).calculateScore());
    }

    @Test
    void factoryReturnsTheRightSubclass() {
        assertInstanceOf(TabSwitchEvent.class, ev(EventType.TAB_SWITCH));
        assertInstanceOf(CopyPasteEvent.class, ev(EventType.COPY_PASTE));
        assertInstanceOf(MultipleLoginEvent.class, ev(EventType.MULTIPLE_LOGIN));
        assertInstanceOf(WindowExitEvent.class, ev(EventType.WINDOW_EXIT));
        assertInstanceOf(PersonDetectionEvent.class, ev(EventType.MULTIPLE_PERSON));
    }

    @Test
    void totalScoreIsTheSumOfAllEvents() {
        ExamMonitor m = monitor(1);
        m.recordEvent(ev(EventType.TAB_SWITCH));
        m.recordEvent(ev(EventType.COPY_PASTE));
        m.recordEvent(ev(EventType.MULTIPLE_PERSON));
        assertEquals(10, m.calculateScore());
    }

    @Test
    void riskBoundariesAt0_4_5_9_10() {
        ExamMonitor m = monitor(1);
        assertEquals("NORMAL", m.classifyRisk());                 // 0
        m.recordEvent(ev(EventType.MULTIPLE_LOGIN));              // 4
        assertEquals(4, m.calculateScore());
        assertEquals("NORMAL", m.classifyRisk());
        m.recordEvent(CheatEventFactory.create(EventType.TAB_SWITCH, T, EventSource.BROWSER_EXTENSION, "tab-1-xxxxx"));
        assertEquals(6, m.calculateScore());                      // jump over 5 below with a separate monitor
        ExamMonitor five = monitor(1);
        five.recordEvent(ev(EventType.TAB_SWITCH));
        five.recordEvent(ev(EventType.COPY_PASTE));               // 5
        assertEquals(5, five.calculateScore());
        assertEquals("SUSPICIOUS", five.classifyRisk());
        ExamMonitor nine = monitor(1);
        nine.recordEvent(ev(EventType.MULTIPLE_LOGIN));
        nine.recordEvent(ev(EventType.MULTIPLE_PERSON));          // 9
        assertEquals(9, nine.calculateScore());
        assertEquals("SUSPICIOUS", nine.classifyRisk());
        ExamMonitor ten = monitor(1);
        ten.recordEvent(ev(EventType.MULTIPLE_PERSON));
        ten.recordEvent(CheatEventFactory.create(EventType.MULTIPLE_PERSON, T, EventSource.SIMULATED_DEMO, "person-2-xxxx"));
        assertEquals(10, ten.calculateScore());
        assertEquals("HIGH RISK", ten.classifyRisk());
    }

    @Test
    void allThreePapersMatchTheDocument() {
        assertEquals(3, PaperBank.all().size());
        String[] names = {"Java Programming", "Data Structures", "DBMS"};
        String[] answerKey = {"CABDD", "CADBC", "ACBCD"};
        for (int i = 1; i <= 3; i++) {
            TestPaper p = PaperBank.getPaper(i);
            assertEquals(names[i - 1], p.getPaperName());
            assertEquals(5, p.getQuestionCount());
            assertEquals(10, p.getTotalMarks());
            for (int q = 0; q < 5; q++) {
                assertEquals(4, p.getQuestion(q).getOptions().length);
                assertEquals(2, p.getQuestion(q).getMarks());
                assertEquals(answerKey[i - 1].charAt(q), p.getQuestion(q).getCorrectOption());
            }
        }
        assertEquals("Which keyword is used to inherit a class in Java?", PaperBank.getPaper(1).getQuestion(0).getText());
        assertEquals("Which data structure follows the LIFO principle?", PaperBank.getPaper(2).getQuestion(0).getText());
        assertEquals("Which SQL clause is used to filter rows?", PaperBank.getPaper(3).getQuestion(4).getText());
    }

    @Test
    void correctAndIncorrectAnswersAndFinalMarks() {
        ExamMonitor m = monitor(1);
        assertTrue(m.submitAnswer(1, 'C'));
        assertTrue(m.submitAnswer(2, 'A'));
        assertFalse(m.submitAnswer(3, 'A'));     // correct is B
        assertTrue(m.submitAnswer(4, 'D'));
        assertTrue(m.submitAnswer(5, 'D'));
        assertEquals(8, m.getMarksObtained());
        assertEquals(5, m.getQuestionsAttempted());
    }

    @Test
    void duplicateAnswerAndBadInputAreRejected() {
        ExamMonitor m = monitor(2);
        m.submitAnswer(1, 'C');
        assertThrows(IllegalStateException.class, () -> m.submitAnswer(1, 'A'));
        assertThrows(InvalidChoiceException.class, () -> m.submitAnswer(2, 'Z'));
        assertThrows(InvalidChoiceException.class, () -> m.submitAnswer(6, 'A'));
        assertThrows(InvalidChoiceException.class, () -> PaperBank.getPaper(4));
    }

    @Test
    void invalidEventTypeIsRejected() {
        assertThrows(InvalidChoiceException.class, () -> EventType.parse("NOPE"));
        assertThrows(InvalidChoiceException.class, () -> EventType.parse(null));
    }

    @Test
    void reportContainsAllRequiredFields() {
        ExamMonitor m = monitor(1);
        m.recordEvent(CheatEventFactory.create(EventType.TAB_SWITCH, T, EventSource.BROWSER_EXTENSION, "tab-1-xxxxx"));
        m.recordEvent(CheatEventFactory.create(EventType.COPY_PASTE, T.plusSeconds(5), EventSource.BROWSER_EXTENSION, "copy-1-xxxx"));
        for (int i = 1; i <= 5; i++) m.submitAnswer(i, "CABDD".charAt(i - 1));
        String r = new Report(m, "sess-123", T, T.plusSeconds(60)).generateReport();
        assertTrue(r.contains("Session ID          : sess-123"));
        assertTrue(r.contains("Student ID          : 1013"));
        assertTrue(r.contains("Student Name        : Shreya"));
        assertTrue(r.contains("Marks Obtained      : 10 / 10"));
        assertTrue(r.contains("Tab Switches        : 1"));
        assertTrue(r.contains("Copy/Paste Attempts : 1"));
        assertTrue(r.contains("Cheat Score         : 5"));
        assertTrue(r.contains("Risk Level          : SUSPICIOUS"));
        assertTrue(r.contains("1. Tab switch at"));
        assertTrue(r.indexOf("Tab switch at") < r.indexOf("Copy/Paste attempt at"));
        assertTrue(r.contains("(+2)") && r.contains("(+3)"));
        assertTrue(r.contains("Session Started") && r.contains("Session Completed"));
    }
}
