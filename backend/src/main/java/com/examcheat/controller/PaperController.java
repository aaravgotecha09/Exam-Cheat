package com.examcheat.controller;

import com.examcheat.dto.Dtos.*;
import com.examcheat.service.PaperService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaperController {
    private final PaperService service;

    public PaperController(PaperService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public java.util.Map<String, String> health() {
        return java.util.Map.of("status", "UP");
    }

    @GetMapping("/papers")
    public List<PaperSummary> papers() {
        return service.list();
    }

    @GetMapping("/papers/{paperId}")
    public PaperDetail paper(@PathVariable int paperId) {
        return service.detail(paperId);
    }
}
