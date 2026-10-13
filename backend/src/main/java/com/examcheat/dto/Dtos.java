package com.examcheat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/** Request / response shapes of the REST API. Entities are never exposed directly. */
public final class Dtos {
    private Dtos() {}

    // ---------------- requests ----------------
    public record RegisterRequest(@NotNull(message = "Student ID is required.") @Positive(message = "Student ID must be a positive number.") Integer studentId,
                                  @NotBlank(message = "Student name is required.") @Size(max = 80, message = "Student name is too long (max 80).") String name) {}

    public record StartRequest(@NotNull(message = "Student ID is required.") @Positive(message = "Student ID must be a positive number.") Integer studentId,
                               @NotNull(message = "Please select a test paper.") Integer paperId,
                               @NotBlank(message = "clientId is required.") @Size(max = 64, message = "clientId is too long.") String clientId) {}

    public record EventRequest(@NotBlank(message = "eventId is required.") @Size(max = 64, message = "eventId is too long.") String eventId,
                               @NotBlank(message = "Event type is required.") String type,
                               @NotBlank(message = "timestamp is required.") String timestamp,
                               String source,
                               @Size(max = 300, message = "details is too long (max 300).") String details) {}

    public record AnswerRequest(@NotNull(message = "questionNumber is required.") Integer questionNumber,
                                @NotBlank(message = "selectedOption is required.") String selectedOption) {}

    // ---------------- responses ----------------
    public record StudentView(int studentId, String name, String registeredAt) {}

    public record PaperSummary(int id, String name, int questionCount, int totalMarks) {}

    public record OptionView(String label, String text) {}

    /** selectedOption is only filled for an exam session; correct answers are never included. */
    public record QuestionView(int number, int totalQuestions, String text, int marks,
                               List<OptionView> options, String selectedOption) {}

    public record PaperDetail(int id, String name, int totalMarks, List<QuestionView> questions) {}

    public record PolicyView(boolean blockClipboard, boolean blockContextMenu) {}

    public record EventView(String eventId, String type, String name, String timestamp, int points, String source) {}

    public record SessionState(String sessionId, int studentId, String studentName, int paperId, String paperName,
                               String status, String startedAt, String finishedAt,
                               int totalQuestions, int attempted, Map<Integer, String> answers,
                               int cheatScore, String risk, int eventCount,
                               Map<String, Integer> eventBreakdown, List<EventView> events,
                               int totalMarks, Integer marksObtained, PolicyView policy) {}

    public record StartResponse(String sessionId, String sessionToken, boolean resumed,
                                boolean multipleLoginRecorded, SessionState state) {}

    public record EventResult(boolean duplicate, int cheatScore, String risk, int eventCount, EventView event) {}

    public record AnswerResult(int questionNumber, int attempted, int totalQuestions) {}

    public record ReviewItem(int number, String question, List<String> options, String selectedOption,
                             String correctOption, boolean correct, int marksAwarded) {}

    public record FinishResult(String sessionId, int marksObtained, int totalMarks, int questionsAttempted,
                               int totalQuestions, int cheatScore, String risk, String reportFile,
                               List<ReviewItem> review, SessionState state) {}

    public record ReportResult(String sessionId, String fileName, String report) {}

    public record ErrorResponse(int status, String error, String message, String timestamp) {}
}
