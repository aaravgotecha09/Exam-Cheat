package com.examcheat.model;

import com.examcheat.event.CheatEvent;
import com.examcheat.exception.InvalidChoiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * OOP concepts:
 *  - ASSOCIATION with Student and TestPaper
 *  - AGGREGATION of CheatEvent objects (List<CheatEvent> = GENERICS / collections)
 *  - RUNTIME POLYMORPHISM: calculateScore() calls the overridden calculateScore() of every event
 *  - implements the Scorable INTERFACE
 *
 * In the web version the monitor is REBUILT from the saved database rows whenever a score is needed,
 * so the domain classes remain the single source of truth for scoring and marking.
 */
public class ExamMonitor implements Scorable {
    private final Student student;
    private final TestPaper paper;
    private final List<CheatEvent> events = new ArrayList<>();
    /** question number (1-based) -> chosen option; at most one answer per question. */
    private final Map<Integer, Character> answers = new TreeMap<>();
    private int marksObtained = 0;

    public ExamMonitor(Student student, TestPaper paper) {
        this.student = student;
        this.paper = paper;
    }

    public void recordEvent(CheatEvent event) {
        events.add(event);
    }

    @Override
    public int calculateScore() {
        int total = 0;
        for (CheatEvent e : events) {
            total += e.calculateScore();          // polymorphic call
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

    public boolean hasAnswered(int questionNumber) {
        return answers.containsKey(questionNumber);
    }

    /**
     * Evaluates one answer on the server. Throws InvalidChoiceException for a bad question number / option
     * and IllegalStateException if the question was already answered. Returns true when correct.
     */
    public boolean submitAnswer(int questionNumber, char answer) {
        if (questionNumber < 1 || questionNumber > paper.getQuestionCount()) {
            throw new InvalidChoiceException("Question number must be between 1 and " + paper.getQuestionCount() + ".");
        }
        if (answer < 'A' || answer > 'D') {
            throw new InvalidChoiceException("Answer must be A, B, C or D.");
        }
        if (answers.containsKey(questionNumber)) {
            throw new IllegalStateException("Question " + questionNumber + " has already been answered.");
        }
        Question q = paper.getQuestion(questionNumber - 1);
        answers.put(questionNumber, answer);
        boolean correct = q.isCorrect(answer);
        if (correct) {
            marksObtained = marksObtained + q.getMarks();
        }
        return correct;
    }

    public Student getStudent() { return student; }
    public TestPaper getPaper() { return paper; }
    public List<CheatEvent> getEvents() { return events; }
    public Map<Integer, Character> getAnswers() { return answers; }
    public int getMarksObtained() { return marksObtained; }
    public int getQuestionsAttempted() { return answers.size(); }
}
