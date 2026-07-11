package com.zinoviev.flowManager.conversion.dao;

import com.zinoviev.flowManager.conversion.model.ConversionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConversionTaskRepository extends JpaRepository<ConversionTask, UUID> {

    List<ConversionTask> findAllByStatusEqualsAndOutboxSentEquals(ConversionTask.TaskStatus status, boolean outboxSent);

    @Modifying
    @Query("""
    UPDATE ConversionTask ct
    SET 
        ct.status = :newStatus,
        ct.updatedAt = :updateAt,
        ct.outboxSent = TRUE
    """)
    void updateStatusAfterSending(@Param("newStatus") ConversionTask.TaskStatus newStatus,
                                  @Param("updateAt") LocalDateTime updateAt);

    @Modifying
    @Query("""
    UPDATE ConversionTask ct
    SET 
        ct.status = :newStatus,
        ct.convertedFileKey = :convertedFileKey,
        ct.errorMessage = :errorMessage,
        ct.lastProcessedEventId = :lastProcessedEventId,
        ct.updatedAt = :updateAt
    WHERE ct.id = :id 
        AND (ct.lastProcessedEventId IS NULL 
             OR ct.lastProcessedEventId <> :lastProcessedEventId))
    """)
    void updateTaskAfterConversion(@Param("id") UUID id,
                                   @Param("newStatus") ConversionTask.TaskStatus newStatus,
                                   @Param("convertedFileKey") String convertedFileKey,
                                   @Param("errorMessage") String errorMessage,
                                   @Param("lastProcessedEventId") UUID lastProcessedEventId);
}
