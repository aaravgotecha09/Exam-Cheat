package com.examcheat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** One answer per (session, question). The unique constraint blocks duplicate submissions even under a race. */
@Entity
@Table(name = "answers", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "question_number"}))
public class AnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(name = "selected_option", nullable = false, length = 1)
    private String selectedOption;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "marks_awarded", nullable = false)
    private Integer marksAwarded;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    protected AnswerEntity() {}

    public AnswerEntity(String sessionId, Integer questionNumber, String selectedOption,
                        boolean correct, Integer marksAwarded, Instant answeredAt) {
        this.sessionId = sessionId;
        this.questionNumber = questionNumber;
        this.selectedOption = selectedOption;
        this.correct = correct;
        this.marksAwarded = marksAwarded;
        this.answeredAt = answeredAt;
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public Integer getQuestionNumber() { return questionNumber; }
    public String getSelectedOption() { return selectedOption; }
    public boolean isCorrect() { return correct; }
    public Integer getMarksAwarded() { return marksAwarded; }
    public Instant getAnsweredAt() { return answeredAt; }
}
