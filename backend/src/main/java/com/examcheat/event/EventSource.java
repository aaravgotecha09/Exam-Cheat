package com.examcheat.event;

/** Where an event came from, so genuine browser signals are never confused with simulated ones. */
public enum EventSource {
    BROWSER_EXTENSION("Browser extension"),
    SIMULATED_DEMO("Simulated demo action"),
    SERVER_RULE("Server rule");

    private final String label;

    EventSource(String label) { this.label = label; }

    public String getLabel() { return label; }
}
