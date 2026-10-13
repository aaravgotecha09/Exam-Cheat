package com.examcheat.model;

/** OOP concept: COMPOSITION. A paper is made of Question objects (array, as in the console project). */
public class TestPaper {
    private final int id;
    private final String paperName;
    private final Question[] questions;

    public TestPaper(int id, String paperName, Question[] questions) {
        this.id = id;
        this.paperName = paperName;
        this.questions = questions;
    }

    public int getId() { return id; }
    public String getPaperName() { return paperName; }
    public int getQuestionCount() { return questions.length; }

    /** @param index 0-based */
    public Question getQuestion(int index) { return questions[index]; }

    public int getTotalMarks() {
        int total = 0;
        for (int i = 0; i < questions.length; i++) {
            total = total + questions[i].getMarks();
        }
        return total;
    }
}
