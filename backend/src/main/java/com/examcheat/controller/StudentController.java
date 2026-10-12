package com.examcheat.controller;

import com.examcheat.dto.Dtos.*;
import com.examcheat.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final ExamService service;

    public StudentController(ExamService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public StudentView register(@Valid @RequestBody RegisterRequest req) {
        return service.register(req);
    }
}
