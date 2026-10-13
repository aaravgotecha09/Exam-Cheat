package com.examcheat.console;

import com.examcheat.analytics.CsvExporter;
import com.examcheat.analytics.ExamAnalytics;
import com.examcheat.event.CheatEvent;
import com.examcheat.event.CheatEventFactory;
import com.examcheat.event.EventSource;
import com.examcheat.event.EventType;
import com.examcheat.exception.InvalidChoiceException;
import com.examcheat.model.ExamMonitor;
import com.examcheat.model.PaperBank;
import com.examcheat.model.Question;
import com.examcheat.model.Report;
import com.examcheat.model.Student;
import com.examcheat.model.TestPaper;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Console edition of the project. It runs on the SAME domain classes as the web backend
 * (Student, TestPaper, ExamMonitor, CheatEvent subclasses, Report), so it needs only javac, no Spring.
 * Menu 1-9 matches the original project; 10-12 are extras (class summary, CSV export, next student).
 */
public class ConsoleApp {
    private final Scanner sc;
    private final List<ExamMonitor> finished = new ArrayList<>();

    public ConsoleApp(Scanner sc) { this.sc = sc; }

    public static void main(String[] args) {
        new ConsoleApp(new Scanner(System.in)).run();
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!sc.hasNextLine()) { System.out.println("\nInput ended. Goodbye."); System.exit(0); }
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    public void run() {
        System.out.println("=== EXAM CHEAT DETECTION SYSTEM (Console) ===");
        boolean another = true;
        while (another) {
            int id = readInt("Enter Student ID: ");
            System.out.print("Enter Student Name: ");
            String name = sc.hasNextLine() ? sc.nextLine().trim() : "";
            TestPaper paper = null;
            while (paper == null) {
                System.out.println("1. Java Programming   2. Data Structures   3. DBMS");
                try {
                    paper = PaperBank.getPaper(readInt("Choose test paper (1-3): "));
                } catch (InvalidChoiceException e) {           // user-defined exception
                    System.out.println("Error: " + e.getMessage());
                }
            }
            Student student = new Student(id, name, paper.getPaperName());
            ExamMonitor monitor = new ExamMonitor(student, paper);
            Instant started = Instant.now();
            boolean running = true;
            while (running) {
                printMenu();
                int choice = readInt("Enter choice: ");
                switch (choice) {
                    case 1: record(monitor, EventType.TAB_SWITCH); break;
                    case 2: record(monitor, EventType.COPY_PASTE); break;
                    case 3: record(monitor, EventType.MULTIPLE_LOGIN); break;
                    case 4: record(monitor, EventType.WINDOW_EXIT); break;
                    case 5: record(monitor, EventType.MULTIPLE_PERSON); break;
                    case 6:
                        System.out.println("Cheat Score: " + monitor.calculateScore() + "  Risk: " + monitor.classifyRisk());
                        break;
                    case 7: report(monitor, started, null); break;
                    case 8: running = false; break;
                    case 9: askNext(monitor); break;
                    case 10: System.out.print(ExamAnalytics.summaryText(withCurrent(monitor))); break;
                    case 11: export(withCurrent(monitor)); break;
                    default: System.out.println("Invalid choice. Enter 1-11.");
                }
            }
            Instant ended = Instant.now();
            finished.add(monitor);
            report(monitor, started, ended);
            System.out.print("Examine another student? (y/n): ");
            another = sc.hasNextLine() && sc.nextLine().trim().equalsIgnoreCase("y");
        }
        System.out.print(ExamAnalytics.summaryText(finished));
        export(finished);
        System.out.println("Thank you.");
    }

    private List<ExamMonitor> withCurrent(ExamMonitor current) {
        List<ExamMonitor> all = new ArrayList<>(finished);
        all.add(current);
        return all;
    }

    private void printMenu() {
        System.out.println("\n--- MENU ---");
        System.out.println("1. Record Tab Switch");
        System.out.println("2. Record Copy/Paste Attempt");
        System.out.println("3. Record Multiple Login");
        System.out.println("4. Record Window Exit");
        System.out.println("5. Record Multiple Person Detection (simulated)");
        System.out.println("6. View Score");
        System.out.println("7. Generate Report");
        System.out.println("8. Exit / Finish");
        System.out.println("9. Attempt Next Question");
        System.out.println("10. Class summary so far");
        System.out.println("11. Export CSV");
    }

    private void record(ExamMonitor m, EventType type) {
        Instant now = Instant.now();
        String id = type + "-" + now.toEpochMilli() + "-" + m.getEvents().size();
        CheatEvent e = CheatEventFactory.create(type, now, EventSource.SIMULATED_DEMO, id);
        m.recordEvent(e);                                       // polymorphism: score comes from the subclass
        System.out.println("Recorded: " + e);
    }

    private void askNext(ExamMonitor m) {
        int next = 1;
        while (m.hasAnswered(next) && next <= m.getPaper().getQuestionCount()) next++;
        if (next > m.getPaper().getQuestionCount()) {
            System.out.println("All questions answered.");
            return;
        }
        Question q = m.getPaper().getQuestion(next - 1);
        System.out.println("Q" + next + ". " + q.getText());
        String[] o = q.getOptions();
        for (int i = 0; i < o.length; i++) System.out.println("  " + (char) ('A' + i) + ". " + o[i]);
        System.out.print("Your answer (A-D): ");
        String line = sc.hasNextLine() ? sc.nextLine().trim().toUpperCase() : "";
        try {
            if (line.length() != 1) throw new InvalidChoiceException("Answer must be A, B, C or D.");
            boolean ok = m.submitAnswer(next, line.charAt(0));
            System.out.println(ok ? "Correct! +" + q.getMarks() : "Wrong.");
        } catch (InvalidChoiceException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void report(ExamMonitor m, Instant started, Instant ended) {
        Report r = new Report(m, "console-" + m.getStudent().getStudentId(), started, ended);
        System.out.println(r.generateReport());
        try {
            String file = "ExamReport_" + m.getStudent().getStudentId() + ".txt";
            r.saveToFile(java.nio.file.Path.of("console-reports"), file);
            System.out.println("Report saved to console-reports/" + file);
        } catch (IOException e) {
            System.out.println("Could not save report: " + e.getMessage());
        }
    }

    private void export(List<ExamMonitor> list) {
        try {
            CsvExporter.save(list, "ExamSummary.csv");
            System.out.println("Summary exported to ExamSummary.csv");
        } catch (IOException e) {
            System.out.println("Could not export CSV: " + e.getMessage());
        }
    }
}
