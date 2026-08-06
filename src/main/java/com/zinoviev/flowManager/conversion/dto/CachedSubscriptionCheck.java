package com.zinoviev.flowManager.conversion.dto;

import java.time.Instant;

public record CachedSubscriptionCheck(
        String userLogin,
        boolean allowed,
        SubscriptionTypeName subscriptionType,
        Long maxFileSizeBytes,
        Instant expiresAt,
        Instant cachedAt
) {
}
