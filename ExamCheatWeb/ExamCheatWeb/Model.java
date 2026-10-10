import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.io.IOException;

// User-defined exception for invalid menu choice / invalid answer
class InvalidChoiceException extends Exception {
    public InvalidChoiceException(String message) {
        super(message);
    }
}

// Interface: contract for objects whose score can be calculated
interface Scorable {
    int calculateScore();
}

// Student class (Encapsulation + Constructor)
class Student {
    private int studentId;
    private String name;
    private String examName;

    public Student(int studentId, String name, String examName) {
        this.studentId = studentId;
        this.name = name;
        this.examName = examName;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }

    public String getExamName() {
        return examName;
    }
}

// Abstract class: common structure of all suspicious events
abstract class CheatEvent implements Scorable {
    protected String timestamp;

    public CheatEvent(String timestamp) {
        this.timestamp = timestamp;
    }

    public abstract int calculateScore();

    public abstract String getEventName();

    public String getTimestamp() {
        return timestamp;
    }
}

// Specialized event classes (Inheritance + Polymorphism)
class TabSwitchEvent extends CheatEvent {
    public TabSwitchEvent(String timestamp) {
        super(timestamp);
    }

    @Override
    public int calculateScore() {
        return 2;
    }

    @Override
    public String getEventName() {
        return "Tab switch";
    }
}

class CopyPasteEvent extends CheatEvent {
    public CopyPasteEvent(String timestamp) {
        super(timestamp);
    }

    @Override
    public int calculateScore() {
        return 3;
    }

    @Override
    public String getEventName() {
        return "Copy/Paste attempt";
    }
}

class MultipleLoginEvent extends CheatEvent {
    public MultipleLoginEvent(String timestamp) {
        super(timestamp);
    }

    @Override
    public int calculateScore() {
        return 4;
    }

    @Override
    public String getEventName() {
        return "Multiple login";
    }
}

class WindowExitEvent extends CheatEvent {
    public WindowExitEvent(String timestamp) {
        super(timestamp);
    }

    @Override
    public int calculateScore() {
        return 3;
    }

    @Override
    public String getEventName() {
        return "Exam window exit";
    }
}

class PersonDetectionEvent extends CheatEvent {
    public PersonDetectionEvent(String timestamp) {
        super(timestamp);
    }

    @Override
    public int calculateScore() {
        return 5;
    }

    @Override
    public String getEventName() {
        return "Multiple person";
    }
}

// Question class: one multiple choice question of a test paper
class Question {
    private String text;
    private String[] options;
    private char correctOption;
    private int marks;

    public Question(String text, String[] options, char correctOption, int marks) {
        this.text = text;
        this.options = options;
        this.correctOption = correctOption;
        this.marks = marks;
    }

    public void display(int number) {
        System.out.println("\nQ" + number + ". " + text + " [" + marks + " marks]");
        for (int i = 0; i < options.length; i++) {
            System.out.println("   " + (char) ('A' + i) + ". " + options[i]);
        }
    }

    public boolean isCorrect(char answer) {
        if (answer == correctOption)
            return true;
        else
            return false;
    }

    public int getMarks() {
        return marks;
    }

    // Getters added for the web front end (the correct answer is never sent to the browser)
    public String getText() {
        return text;
    }

    public String[] getOptions() {
        return options;
    }
}

// TestPaper class: a paper is made up of Question objects
class TestPaper {
    private String paperName;
    private Question[] questions;

    public TestPaper(String paperName, Question[] questions) {
        this.paperName = paperName;
        this.questions = questions;
    }

    public String getPaperName() {
        return paperName;
    }

    public int getQuestionCount() {
        return questions.length;
    }

    public Question getQuestion(int index) {
        return questions[index];
    }

    public int getTotalMarks() {
        int total = 0;
        for (int i = 0; i < questions.length; i++) {
            total = total + questions[i].getMarks();
        }
        return total;
    }
}

// PaperBank class: stores the 3 sample test papers
class PaperBank {
    // Helper method to create a question worth 2 marks
    static Question makeQuestion(String text, String a, String b, String c, String d, char correct) {
        String[] opts = {a, b, c, d};
        return new Question(text, opts, correct, 2);
    }

    static String[] paperNames() {
        return new String[]{"Java Programming", "Data Structures", "DBMS"};
    }

    static void showPapers() {
        System.out.println("\n===== AVAILABLE TEST PAPERS =====");
        System.out.println("1. Java Programming   (5 questions, 10 marks)");
        System.out.println("2. Data Structures    (5 questions, 10 marks)");
        System.out.println("3. DBMS               (5 questions, 10 marks)");
    }

    static TestPaper getPaper(int choice) {
        Question[] q = new Question[5];
        if (choice == 1) {
            q[0] = makeQuestion("Which keyword is used to inherit a class in Java?",
                    "implements", "super", "extends", "inherits", 'C');
            q[1] = makeQuestion("Which feature is used to achieve runtime polymorphism?",
                    "Method overriding", "Method overloading", "Constructor overloading", "Static binding", 'A');
            q[2] = makeQuestion("Which keyword is used to create an object?",
                    "class", "new", "this", "object", 'B');
            q[3] = makeQuestion("Which block always executes whether or not an exception occurs?",
                    "try", "catch", "throw", "finally", 'D');
            q[4] = makeQuestion("A class that cannot be instantiated and may contain abstract methods is called?",
                    "Interface", "Final class", "Static class", "Abstract class", 'D');
            return new TestPaper("Java Programming", q);
        }
        else if (choice == 2) {
            q[0] = makeQuestion("Which data structure follows the LIFO principle?",
                    "Queue", "Array", "Stack", "Tree", 'C');
            q[1] = makeQuestion("Which data structure follows the FIFO principle?",
                    "Queue", "Stack", "Graph", "Tree", 'A');
            q[2] = makeQuestion("What is the worst case time complexity of binary search?",
                    "O(n)", "O(n^2)", "O(1)", "O(log n)", 'D');
            q[3] = makeQuestion("Which stack operation removes the top element?",
                    "push", "pop", "peek", "insert", 'B');
            q[4] = makeQuestion("In a singly linked list, each node contains data and what else?",
                    "An index number", "A hash value", "A reference to the next node", "The array size", 'C');
            return new TestPaper("Data Structures", q);
        }
        else {
            q[0] = makeQuestion("Which SQL command is used to retrieve data from a table?",
                    "SELECT", "INSERT", "UPDATE", "DELETE", 'A');
            q[1] = makeQuestion("A primary key must contain?",
                    "Duplicate values", "NULL values", "Unique and non-null values", "Only numbers", 'C');
            q[2] = makeQuestion("Which normal form removes partial dependency?",
                    "1NF", "2NF", "3NF", "BCNF", 'B');
            q[3] = makeQuestion("Which command removes a table completely from the database?",
                    "DELETE", "TRUNCATE", "DROP", "REMOVE", 'C');
            q[4] = makeQuestion("Which SQL clause is used to filter rows?",
                    "GROUP BY", "ORDER BY", "JOIN", "WHERE", 'D');
            return new TestPaper("DBMS", q);
        }
    }
}

// ExamMonitor class
// Association with Student, Aggregation of CheatEvent objects, Generics (List)
class ExamMonitor implements Scorable {
    private Student student;
    private TestPaper paper;
    private List<CheatEvent> events = new ArrayList<>();
    private int currentQuestion = 0;
    private int marksObtained = 0;

    public ExamMonitor(Student student, TestPaper paper) {
        this.student = student;
        this.paper = paper;
    }

    public void recordEvent(CheatEvent event) {
        events.add(event);
    }

    // Polymorphism: the overridden calculateScore() of each event is called
    @Override
    public int calculateScore() {
        int total = 0;
        for (CheatEvent e : events) {
            total += e.calculateScore();
        }
        return total;
    }

    public String classifyRisk() {
        int score = calculateScore();
        if (score <= 4)
            return "NORMAL";
        else if (score <= 9)
            return "SUSPICIOUS";
        else
            return "HIGH RISK";
    }

    public int countEvents(String eventName) {
        int count = 0;
        for (CheatEvent e : events) {
            if (e.getEventName().equals(eventName)) {
                count++;
            }
        }
        return count;
    }

    public boolean hasMoreQuestions() {
        if (currentQuestion < paper.getQuestionCount())
            return true;
        else
            return false;
    }

    public Question getCurrentQuestion() {
        return paper.getQuestion(currentQuestion);
    }

    public int getCurrentNumber() {
        return currentQuestion + 1;
    }

    public void submitAnswer(char answer) {
        Question q = paper.getQuestion(currentQuestion);
        if (q.isCorrect(answer)) {
            marksObtained = marksObtained + q.getMarks();
        }
        currentQuestion++;
    }

    public Student getStudent() {
        return student;
    }

    public TestPaper getPaper() {
        return paper;
    }

    public List<CheatEvent> getEvents() {
        return events;
    }

    public int getMarksObtained() {
        return marksObtained;
    }

    public int getQuestionsAttempted() {
        return currentQuestion;
    }
}

// Report class: builds the report from the ExamMonitor and saves it to a file
class Report {
    private ExamMonitor monitor;

    public Report(ExamMonitor monitor) {
        this.monitor = monitor;
    }

    public String generateReport() {
        Student s = monitor.getStudent();
        TestPaper p = monitor.getPaper();
        String r = "===== EXAM CHEAT DETECTION REPORT =====\n"
                + "Student ID          : " + s.getStudentId() + "\n"
                + "Student Name        : " + s.getName() + "\n"
                + "Exam                : " + s.getExamName() + "\n"
                + "Questions Attempted : " + monitor.getQuestionsAttempted() + " / " + p.getQuestionCount() + "\n"
                + "Marks Obtained      : " + monitor.getMarksObtained() + " / " + p.getTotalMarks() + "\n"
                + "Tab Switches        : " + monitor.countEvents("Tab switch") + "\n"
                + "Copy/Paste Attempts : " + monitor.countEvents("Copy/Paste attempt") + "\n"
                + "Multiple Logins     : " + monitor.countEvents("Multiple login") + "\n"
                + "Window Exits        : " + monitor.countEvents("Exam window exit") + "\n"
                + "Person Detection    : " + monitor.countEvents("Multiple person") + "\n"
                + "Cheat Score         : " + monitor.calculateScore() + "\n"
                + "Risk Level          : " + monitor.classifyRisk() + "\n";

        // Event log
        r = r + "----------- Event Log -----------\n";
        List<CheatEvent> list = monitor.getEvents();
        if (list.size() == 0) {
            r = r + "No suspicious events recorded.\n";
        }
        else {
            for (int i = 0; i < list.size(); i++) {
                CheatEvent e = list.get(i);
                r = r + (i + 1) + ". " + e.getEventName() + " at " + e.getTimestamp()
                        + " (+" + e.calculateScore() + ")\n";
            }
        }
        r = r + "==========================================";
        return r;
    }

    // File handling
    public void saveToFile(String filename) {
        try (FileWriter writer = new FileWriter(filename, true)) {
            writer.write(generateReport() + "\n\n");
            System.out.println("Report saved to " + filename);
        }
        catch (IOException e) {
            System.out.println("Could not save report: " + e.getMessage());
        }
    }
}
