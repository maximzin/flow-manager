package com.zinoviev.flowManager.conversion.cache.service;

import com.zinoviev.flowManager.conversion.dto.SubscriptionCheckResultDto;

public interface SubscriptionCacheService {

    SubscriptionCheckResultDto checkSubscription(String userLogin, long fileSizeBytes);

    void invalidateCache(String userLogin);

}
