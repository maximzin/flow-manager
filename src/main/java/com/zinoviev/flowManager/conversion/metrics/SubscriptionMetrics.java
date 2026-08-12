package com.zinoviev.flowManager.conversion.metrics;

import com.zinoviev.flowManager.conversion.dto.SubscriptionTypeName;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionMetrics {
    private final MeterRegistry registry;

    public SubscriptionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordProcessedTask(SubscriptionTypeName subscriptionTypeName) {
        Counter.builder("flow_manager_tasks_by_subscription_total")
                .description("Number of processed tasks by subscription type")
                .tag("subscription_type", subscriptionTypeName.name().toLowerCase())
                .register(registry)
                .increment();
    }
}
