package com.examcheat.event;

import java.time.Instant;

/** OOP concepts: INHERITANCE (extends CheatEvent) + method OVERRIDING (runtime polymorphism). */
public class CopyPasteEvent extends CheatEvent {
    public CopyPasteEvent(Instant occurredAt, EventSource source, String eventId) {
        super(occurredAt, source, eventId);
    }

    @Override
    public int calculateScore() {
        return 3;
    }

    @Override
    public String getEventName() {
        return "Copy/Paste attempt";
    }

    @Override
    public EventType getType() {
        return EventType.COPY_PASTE;
    }
}
