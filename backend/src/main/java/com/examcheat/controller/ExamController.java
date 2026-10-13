package com.examcheat.controller;

import com.examcheat.dto.Dtos.*;
import com.examcheat.service.ExamService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Every endpoint except /start needs the X-Session-Token returned by /start (demo authentication). */
@RestController
@RequestMapping("/api/exams")
public class ExamController {
    private static final String TOKEN = "X-Session-Token";
    private final ExamService service;

    public ExamController(ExamService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public StartResponse start(@Valid @RequestBody StartRequest req) {
        return service.start(req);
    }

    @GetMapping("/{sessionId}")
    public SessionState state(@PathVariable String sessionId,
                              @RequestHeader(value = TOKEN, required = false) String token) {
        return service.getState(sessionId, token);
    }

    @GetMapping("/{sessionId}/questions/{number}")
    public QuestionView question(@PathVariable String sessionId, @PathVariable int number,
                                 @RequestHeader(value = TOKEN, required = false) String token) {
        return service.getQuestion(sessionId, token, number);
    }

    @PostMapping("/{sessionId}/events")
    public EventResult event(@PathVariable String sessionId, @Valid @RequestBody EventRequest req,
                             @RequestHeader(value = TOKEN, required = false) String token) {
        return service.recordEvent(sessionId, token, req);
    }

    @PostMapping("/{sessionId}/answers")
    public AnswerResult answer(@PathVariable String sessionId, @Valid @RequestBody AnswerRequest req,
                               @RequestHeader(value = TOKEN, required = false) String token) {
        return service.answer(sessionId, token, req);
    }

    @PostMapping("/{sessionId}/finish")
    public FinishResult finish(@PathVariable String sessionId,
                               @RequestHeader(value = TOKEN, required = false) String token) {
        return service.finish(sessionId, token);
    }

    @GetMapping("/{sessionId}/report")
    public ReportResult report(@PathVariable String sessionId,
                               @RequestHeader(value = TOKEN, required = false) String token) {
        return service.report(sessionId, token);
    }

    @GetMapping("/{sessionId}/report/download")
    public ResponseEntity<byte[]> download(@PathVariable String sessionId,
                                           @RequestHeader(value = TOKEN, required = false) String token) {
        String[] file = service.reportForDownload(sessionId, token);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + file[0] + "\"")
                .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                .body(file[1].getBytes(StandardCharsets.UTF_8));
    }
}
