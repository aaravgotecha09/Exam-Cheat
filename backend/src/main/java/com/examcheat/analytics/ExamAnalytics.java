package com.examcheat.analytics;

import com.examcheat.event.EventType;
import com.examcheat.model.ExamMonitor;
import com.examcheat.model.RiskLevel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Summary statistics over many exam sessions. Uses GENERICS (List<ExamMonitor>, EnumMap), COLLECTIONS and
 * a COMPARATOR. Pure Java: works for the console app and could be used by the web backend too.
 */
public final class ExamAnalytics {
    private ExamAnalytics() {}

    public static Map<RiskLevel, Integer> riskDistribution(List<ExamMonitor> sessions) {
        Map<RiskLevel, Integer> map = new EnumMap<>(RiskLevel.class);
        for (RiskLevel r : RiskLevel.values()) map.put(r, 0);
        for (ExamMonitor m : sessions) map.merge(m.getRiskLevel(), 1, Integer::sum);
        return map;
    }

    public static Map<EventType, Integer> eventTotals(List<ExamMonitor> sessions) {
        Map<EventType, Integer> map = new EnumMap<>(EventType.class);
        for (EventType t : EventType.values()) map.put(t, 0);
        for (ExamMonitor m : sessions)
            for (EventType t : EventType.values()) map.merge(t, m.countEvents(t), Integer::sum);
        return map;
    }

    public static double averageScore(List<ExamMonitor> sessions) {
        if (sessions.isEmpty()) return 0;
        int total = 0;
        for (ExamMonitor m : sessions) total += m.calculateScore();
        return (double) total / sessions.size();
    }

    public static double averageMarks(List<ExamMonitor> sessions) {
        if (sessions.isEmpty()) return 0;
        int total = 0;
        for (ExamMonitor m : sessions) total += m.getMarksObtained();
        return (double) total / sessions.size();
    }

    /** Sessions sorted by cheat score, highest first. */
    public static List<ExamMonitor> rankByScore(List<ExamMonitor> sessions) {
        List<ExamMonitor> copy = new ArrayList<>(sessions);
        copy.sort(Comparator.comparingInt(ExamMonitor::calculateScore).reversed());
        return copy;
    }

    public static String summaryText(List<ExamMonitor> sessions) {
        StringBuilder sb = new StringBuilder("===== CLASS SUMMARY =====\n");
        sb.append("Students examined   : ").append(sessions.size()).append('\n');
        sb.append(String.format("Average cheat score : %.2f%n", averageScore(sessions)));
        sb.append(String.format("Average marks       : %.2f%n", averageMarks(sessions)));
        sb.append("Risk distribution   : ");
        for (Map.Entry<RiskLevel, Integer> e : riskDistribution(sessions).entrySet())
            sb.append(e.getKey().getLabel()).append('=').append(e.getValue()).append("  ");
        sb.append("\nEvent totals        : ");
        for (Map.Entry<EventType, Integer> e : eventTotals(sessions).entrySet())
            sb.append(e.getKey()).append('=').append(e.getValue()).append("  ");
        sb.append("\nRanking (highest cheat score first):\n");
        int rank = 1;
        for (ExamMonitor m : rankByScore(sessions))
            sb.append("  ").append(rank++).append(". ").append(m.getStudent().getName())
              .append(" - score ").append(m.calculateScore()).append(" (").append(m.classifyRisk()).append(")\n");
        return sb.toString();
    }
}
