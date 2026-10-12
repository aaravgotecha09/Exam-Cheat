package com.examcheat.event;

import java.time.Instant;

/** OOP concepts: INHERITANCE (extends CheatEvent) + method OVERRIDING (runtime polymorphism). */
public class TabSwitchEvent extends CheatEvent {
    public TabSwitchEvent(Instant occurredAt, EventSource source, String eventId) {
        super(occurredAt, source, eventId);
    }

    @Override
    public int calculateScore() {
        return 2;
    }

    @Override
    public String getEventName() {
        return "Tab switch";
    }

    @Override
    public EventType getType() {
        return EventType.TAB_SWITCH;
    }
}
