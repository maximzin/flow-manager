package com.zinoviev.flowManager.conversion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversion_task")
@Getter
@Setter
@NoArgsConstructor
public class ConversionTask {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TaskStatus status;

    @Column(name = "original_file_key", nullable = false)
    private String originalFileKey;

    @Column(name = "converted_file_key")
    private String convertedFileKey;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "outbox_sent", nullable = false)
    private Boolean outboxSent;

    @Column(name = "last_processed_event_id")
    private UUID lastProcessedEventId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TaskStatus {
        UPLOADING,
        UPLOADED,
        PENDING,
        COMPLETED,
        FAILED
    }

    public ConversionTask(UUID id, String originalFileKey) {
        this.id = id;
        this.status = TaskStatus.UPLOADED;
        this.originalFileKey = originalFileKey;
        this.outboxSent = false;
    }
}
