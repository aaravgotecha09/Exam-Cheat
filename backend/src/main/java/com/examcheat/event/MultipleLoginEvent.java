package com.examcheat.event;

import java.time.Instant;

/** OOP concepts: INHERITANCE (extends CheatEvent) + method OVERRIDING (runtime polymorphism). */
public class MultipleLoginEvent extends CheatEvent {
    public MultipleLoginEvent(Instant occurredAt, EventSource source, String eventId) {
        super(occurredAt, source, eventId);
    }

    @Override
    public int calculateScore() {
        return 4;
    }

    @Override
    public String getEventName() {
        return "Multiple login";
    }

    @Override
    public EventType getType() {
        return EventType.MULTIPLE_LOGIN;
    }
}
