package com.examcheat.event;

import com.examcheat.exception.InvalidChoiceException;

/** Wire names used by the REST API, the extension and the database. */
public enum EventType {
    TAB_SWITCH, COPY_PASTE, MULTIPLE_LOGIN, WINDOW_EXIT, MULTIPLE_PERSON;

    public static EventType parse(String text) {
        if (text == null) {
            throw new InvalidChoiceException("Event type is required.");
        }
        try {
            return EventType.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidChoiceException("Unknown event type: " + text);
        }
    }
}
