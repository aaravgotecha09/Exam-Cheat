package com.examcheat.event;

import java.time.Instant;

/** OOP concepts: INHERITANCE (extends CheatEvent) + method OVERRIDING (runtime polymorphism). */
public class WindowExitEvent extends CheatEvent {
    public WindowExitEvent(Instant occurredAt, EventSource source, String eventId) {
        super(occurredAt, source, eventId);
    }

    @Override
    public int calculateScore() {
        return 3;
    }

    @Override
    public String getEventName() {
        return "Exam window exit";
    }

    @Override
    public EventType getType() {
        return EventType.WINDOW_EXIT;
    }
}
