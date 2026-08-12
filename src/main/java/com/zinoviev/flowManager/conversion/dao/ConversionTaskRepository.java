package com.zinoviev.flowManager.conversion.dao;

import com.zinoviev.flowManager.conversion.model.ConversionTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConversionTaskRepository extends JpaRepository<ConversionTask, UUID> {

    List<ConversionTask> findAllByStatusEqualsAndOutboxSentEquals(ConversionTask.TaskStatus status, boolean outboxSent);

    List<ConversionTask> findByStatusAndCreatedAtBefore(ConversionTask.TaskStatus taskStatus, LocalDateTime threshold);

}
