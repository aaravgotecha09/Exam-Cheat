package com.examcheat.repository;

import com.examcheat.entity.ExamSessionEntity;
import com.examcheat.entity.SessionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamSessionRepository extends JpaRepository<ExamSessionEntity, String> {
    List<ExamSessionEntity> findByStudentIdAndStatusOrderByStartedAtAsc(Integer studentId, SessionStatus status);
}
