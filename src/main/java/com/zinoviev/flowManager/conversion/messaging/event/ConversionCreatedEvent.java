package com.zinoviev.flowManager.conversion.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversionCreatedEvent(
        UUID eventId,
        String originalFileKey,
        LocalDateTime createdAt
) {}
