package com.examcheat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.examcheat.analytics.CsvExporter;
import com.examcheat.analytics.ExamAnalytics;
import com.examcheat.event.CheatEventFactory;
import com.examcheat.event.EventSource;
import com.examcheat.event.EventType;
import com.examcheat.model.ExamMonitor;
import com.examcheat.model.PaperBank;
import com.examcheat.model.RiskLevel;
import com.examcheat.model.Student;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyticsTest {
    private ExamMonitor monitor(int id, String name, EventType... types) {
        ExamMonitor m = new ExamMonitor(new Student(id, name), PaperBank.getPaper(1));
        int n = 0;
        for (EventType t : types) {
            m.recordEvent(CheatEventFactory.create(t, Instant.now(), EventSource.SIMULATED_DEMO, t + "-" + n++));
        }
        return m;
    }

    @Test
    void riskLevelThresholds() {
        assertEquals(RiskLevel.NORMAL, RiskLevel.fromScore(4));
        assertEquals(RiskLevel.SUSPICIOUS, RiskLevel.fromScore(5));
        assertEquals(RiskLevel.SUSPICIOUS, RiskLevel.fromScore(9));
        assertEquals(RiskLevel.HIGH_RISK, RiskLevel.fromScore(10));
        assertEquals("HIGH RISK", RiskLevel.HIGH_RISK.getLabel());
    }

    @Test
    void countByTypeAndRanking() {
        ExamMonitor a = monitor(1, "A", EventType.TAB_SWITCH, EventType.TAB_SWITCH);
        ExamMonitor b = monitor(2, "B", EventType.MULTIPLE_PERSON, EventType.COPY_PASTE, EventType.MULTIPLE_LOGIN);
        assertEquals(2, a.countEvents(EventType.TAB_SWITCH));
        List<ExamMonitor> all = List.of(a, b);
        assertEquals(12, b.calculateScore());
        assertEquals("B", ExamAnalytics.rankByScore(all).get(0).getStudent().getName());
        assertEquals(1, ExamAnalytics.riskDistribution(all).get(RiskLevel.HIGH_RISK));
        assertEquals(1, ExamAnalytics.riskDistribution(all).get(RiskLevel.NORMAL));
        assertEquals(8.0, ExamAnalytics.averageScore(all), 0.001);
        assertEquals(0.0, ExamAnalytics.averageScore(List.of()), 0.001);
    }

    @Test
    void csvIsEscapedAgainstFormulaInjection() {
        ExamMonitor m = monitor(3, "=cmd|' /C calc'!A0", EventType.WINDOW_EXIT);
        String csv = CsvExporter.toCsv(List.of(m));
        assertTrue(csv.startsWith("student_id,name"));
        assertTrue(csv.contains("\"'=cmd"));
        assertTrue(csv.contains(",3,\"NORMAL\""));
    }
}
