package com.examcheat.model;

/** One multiple choice question: 4 options, one correct answer, marks. The answer key never leaves the server. */
public class Question {
    private final String text;
    private final String[] options;
    private final char correctOption;
    private final int marks;

    public Question(String text, String[] options, char correctOption, int marks) {
        this.text = text;
        this.options = options;
        this.correctOption = correctOption;
        this.marks = marks;
    }

    public boolean isCorrect(char answer) { return answer == correctOption; }
    public String getText() { return text; }
    public String[] getOptions() { return options.clone(); }
    public int getMarks() { return marks; }
    /** Used only server side (marking and the review shown AFTER the exam is finished). */
    public char getCorrectOption() { return correctOption; }
}
