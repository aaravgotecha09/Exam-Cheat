package com.examcheat.analytics;

import com.examcheat.model.ExamMonitor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/** FILE HANDLING: exports all sessions to a CSV file that opens in Excel. */
public final class CsvExporter {
    private CsvExporter() {}

    public static String toCsv(List<ExamMonitor> sessions) {
        StringBuilder sb = new StringBuilder("student_id,name,exam,marks,total_marks,tab_switch,copy_paste,multiple_login,window_exit,multiple_person,cheat_score,risk\n");
        for (ExamMonitor m : sessions) {
            sb.append(m.getStudent().getStudentId()).append(',')
              .append(escape(m.getStudent().getName())).append(',')
              .append(escape(m.getPaper().getPaperName())).append(',')
              .append(m.getMarksObtained()).append(',').append(m.getPaper().getTotalMarks()).append(',');
            for (com.examcheat.event.EventType t : com.examcheat.event.EventType.values())
                sb.append(m.countEvents(t)).append(',');
            sb.append(m.calculateScore()).append(',').append(escape(m.classifyRisk())).append('\n');
        }
        return sb.toString();
    }

    /** Quotes a cell and doubles inner quotes; also prefixes formula characters to avoid CSV injection in Excel. */
    static String escape(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return '"' + s.replace("\"", "\"\"") + '"';
    }

    public static void save(List<ExamMonitor> sessions, String path) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            out.print(toCsv(sessions));
        }
    }
}
