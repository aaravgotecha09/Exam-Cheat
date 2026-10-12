package com.examcheat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One exam attempt. The session token is stored only as a SHA-256 hash. */
@Entity
@Table(name = "exam_sessions")
public class ExamSessionEntity {
    @Id
    @Column(name = "session_id", length = 36)
    private String id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "student_name", nullable = false, length = 80)
    private String studentName;

    @Column(name = "paper_id", nullable = false)
    private Integer paperId;

    /** Identifies the browser tab that started the exam (a refresh keeps it, a second tab gets a new one). */
    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected ExamSessionEntity() {}

    public ExamSessionEntity(String id, Integer studentId, String studentName, Integer paperId,
                             String clientId, String tokenHash, Instant now) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.paperId = paperId;
        this.clientId = clientId;
        this.tokenHash = tokenHash;
        this.status = SessionStatus.IN_PROGRESS;
        this.startedAt = now;
        this.lastSeenAt = now;
    }

    public String getId() { return id; }
    public Integer getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public Integer getPaperId() { return paperId; }
    public String getClientId() { return clientId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
