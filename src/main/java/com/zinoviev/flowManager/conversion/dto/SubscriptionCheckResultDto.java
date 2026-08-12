package com.zinoviev.flowManager.conversion.dto;

import java.time.Instant;

public record SubscriptionCheckResultDto(

        String userLogin,
        boolean allowed,

        SubscriptionTypeName subscriptionTypeName,

        Long maxFileSizeBytes,

        Instant expiresAt,

        String message

) {
}

