package com.examcheat.event;

import java.time.Instant;

/** OOP concepts: INHERITANCE (extends CheatEvent) + method OVERRIDING (runtime polymorphism). */
public class PersonDetectionEvent extends CheatEvent {
    public PersonDetectionEvent(Instant occurredAt, EventSource source, String eventId) {
        super(occurredAt, source, eventId);
    }

    @Override
    public int calculateScore() {
        return 5;
    }

    @Override
    public String getEventName() {
        return "Multiple person";
    }

    @Override
    public EventType getType() {
        return EventType.MULTIPLE_PERSON;
    }
}
