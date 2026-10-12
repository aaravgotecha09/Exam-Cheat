package com.examcheat.event;

import java.time.Instant;

/**
 * Creates the right CheatEvent subclass for an event type. The caller only sees the CheatEvent
 * reference (RUNTIME POLYMORPHISM): the points come from the subclass, never from the browser.
 */
public final class CheatEventFactory {
    private CheatEventFactory() {}

    public static CheatEvent create(EventType type, Instant at, EventSource source, String eventId) {
        switch (type) {
            case TAB_SWITCH:      return new TabSwitchEvent(at, source, eventId);
            case COPY_PASTE:      return new CopyPasteEvent(at, source, eventId);
            case MULTIPLE_LOGIN:  return new MultipleLoginEvent(at, source, eventId);
            case WINDOW_EXIT:     return new WindowExitEvent(at, source, eventId);
            case MULTIPLE_PERSON: return new PersonDetectionEvent(at, source, eventId);
            default: throw new IllegalArgumentException("Unsupported event type " + type);
        }
    }
}
