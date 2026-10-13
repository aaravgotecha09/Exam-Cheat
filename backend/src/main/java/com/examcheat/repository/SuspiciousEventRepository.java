package com.examcheat.repository;

import com.examcheat.entity.SuspiciousEventEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuspiciousEventRepository extends JpaRepository<SuspiciousEventEntity, Long> {
    List<SuspiciousEventEntity> findBySessionIdOrderByOccurredAtAscIdAsc(String sessionId);

    Optional<SuspiciousEventEntity> findBySessionIdAndEventId(String sessionId, String eventId);
}
