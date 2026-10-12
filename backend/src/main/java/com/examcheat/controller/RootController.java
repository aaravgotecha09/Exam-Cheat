package com.examcheat.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Friendly answer when you open the Render URL in a browser. */
@RestController
public class RootController {
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("service", "Exam Cheat Detection API", "status", "UP", "health", "/api/health", "papers", "/api/papers");
    }
}
