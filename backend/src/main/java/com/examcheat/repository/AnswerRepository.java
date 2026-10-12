package com.examcheat.repository;

import com.examcheat.entity.AnswerEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<AnswerEntity, Long> {
    List<AnswerEntity> findBySessionIdOrderByQuestionNumberAsc(String sessionId);

    boolean existsBySessionIdAndQuestionNumber(String sessionId, Integer questionNumber);
}
