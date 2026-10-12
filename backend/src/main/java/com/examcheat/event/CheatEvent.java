package com.examcheat.event;

import com.examcheat.model.Scorable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * OOP concepts: ABSTRACT CLASS (common structure of all suspicious events), implements the Scorable
 * INTERFACE, ENCAPSULATION (protected state + getters).
 */
public abstract class CheatEvent implements Scorable {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    protected final Instant occurredAt;
    protected final EventSource source;
    protected final String eventId;

    protected CheatEvent(Instant occurredAt, EventSource source, String eventId) {
        this.occurredAt = occurredAt;
        this.source = source;
        this.eventId = eventId;
    }

    @Override
    public abstract int calculateScore();

    public abstract String getEventName();

    public abstract EventType getType();

    public Instant getOccurredAt() { return occurredAt; }
    public EventSource getSource() { return source; }
    public String getEventId() { return eventId; }

    /** Human readable timestamp used in reports. */
    public String getTimestamp() { return format(occurredAt); }

    public static String format(Instant instant) {
        return FORMAT.format(instant);
    }
}
