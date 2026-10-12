package com.examcheat.service;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Basic throttling: at most N events per session per minute (sliding window). */
@Component
public class EventRateLimiter {
    private final int maxPerMinute;
    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new HashMap<>();

    public EventRateLimiter(@Value("${app.max-events-per-minute:60}") int maxPerMinute, Clock clock) {
        this.maxPerMinute = maxPerMinute;
        this.clock = clock;
    }

    public synchronized boolean tryAcquire(String key) {
        long now = clock.millis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!q.isEmpty() && now - q.peekFirst() > 60_000) {
            q.pollFirst();
        }
        if (q.size() >= maxPerMinute) {
            return false;
        }
        q.addLast(now);
        return true;
    }
}
