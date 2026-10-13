package com.examcheat.service;

import com.examcheat.entity.ExamSessionEntity;
import com.examcheat.exception.ApiException;
import com.examcheat.model.ExamMonitor;
import com.examcheat.model.Report;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** File handling: one report file per session in the reports directory. */
@Service
public class ReportService {
    private final Path directory;

    public ReportService(@Value("${app.reports-dir:../reports}") String directory) {
        this.directory = Paths.get(directory).toAbsolutePath().normalize();
    }

    public String fileName(ExamSessionEntity s) {
        return "ExamReport_" + s.getStudentId() + "_" + s.getId().substring(0, 8) + ".txt";
    }

    public Path getDirectory() {
        return directory;
    }

    public String render(ExamSessionEntity s, ExamMonitor monitor) {
        return new Report(monitor, s.getId(), s.getStartedAt(), s.getFinishedAt()).generateReport();
    }

    /** Writes the report file and returns its text. */
    public String save(ExamSessionEntity s, ExamMonitor monitor) {
        try {
            new Report(monitor, s.getId(), s.getStartedAt(), s.getFinishedAt()).saveToFile(directory, fileName(s));
        } catch (IOException e) {
            throw new ApiException(500, "Could not save the report file: " + e.getMessage());
        }
        return render(s, monitor);
    }
}
