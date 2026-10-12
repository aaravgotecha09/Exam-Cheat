package com.examcheat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "students")
public class StudentEntity {
    @Id
    @Column(name = "student_id")
    private Integer studentId;

    @Column(name = "student_name", nullable = false, length = 80)
    private String name;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    protected StudentEntity() {}

    public StudentEntity(Integer studentId, String name, Instant registeredAt) {
        this.studentId = studentId;
        this.name = name;
        this.registeredAt = registeredAt;
    }

    public Integer getStudentId() { return studentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getRegisteredAt() { return registeredAt; }
}
