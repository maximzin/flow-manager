package com.zinoviev.flowManager.conversion.api;

import com.zinoviev.flowManager.conversion.dto.SubscriptionCheckRequestDto;
import com.zinoviev.flowManager.conversion.dto.SubscriptionCheckResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "subscription-service")
public interface SubscriptionServiceClient {

    @PostMapping("/api/v1/subscriptions/check")
    SubscriptionCheckResultDto checkSubscription(
            @RequestHeader("X-User-Login") String userLogin,
            @RequestBody SubscriptionCheckRequestDto request
    );
}
