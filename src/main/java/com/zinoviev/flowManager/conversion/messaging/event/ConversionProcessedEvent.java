package com.zinoviev.flowManager.conversion.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversionProcessedEvent(
        UUID eventId,
        ConversionEventStatus status,
        String originalFileKey,
        String convertedFileKey,
        String errorMessage,
        LocalDateTime createdAt
) {}
