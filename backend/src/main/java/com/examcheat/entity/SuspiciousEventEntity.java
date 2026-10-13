package com.examcheat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * A recorded suspicious event. (session_id, event_id) is unique: that is what makes event
 * processing IDEMPOTENT, so a retried request can never score twice.
 */
@Entity
@Table(name = "suspicious_events", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "event_id"}))
public class SuspiciousEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "event_source", nullable = false, length = 30)
    private String eventSource;

    @Column(name = "details", length = 300)
    private String details;

    protected SuspiciousEventEntity() {}

    public SuspiciousEventEntity(String sessionId, String eventId, String eventType, Instant occurredAt,
                                 Instant receivedAt, Integer points, String eventSource, String details) {
        this.sessionId = sessionId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
        this.points = points;
        this.eventSource = eventSource;
        this.details = details;
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public Integer getPoints() { return points; }
    public String getEventSource() { return eventSource; }
    public String getDetails() { return details; }
}
