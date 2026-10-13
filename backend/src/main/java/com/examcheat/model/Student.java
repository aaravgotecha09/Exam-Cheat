package com.examcheat.model;

/** OOP concepts: ENCAPSULATION (private final fields, getters) + CONSTRUCTOR. */
public class Student {
    private final int studentId;
    private final String name;
    private final String examName;

    public Student(int studentId, String name, String examName) {
        this.studentId = studentId;
        this.name = name;
        this.examName = examName;
    }

    /** CONSTRUCTOR OVERLOADING: console version registers a student without choosing an exam name yet. */
    public Student(int studentId, String name) {
        this(studentId, name, "Online Exam");
    }

    public int getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getExamName() { return examName; }
}
