package com.zinoviev.flowManager.conversion.messaging.handler;

import com.zinoviev.flowManager.conversion.cache.service.SubscriptionCacheService;
import com.zinoviev.flowManager.conversion.messaging.event.SubscriptionInvalidatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
@KafkaListener(
        topics = "#{kafkaConsumerProperties.consumers['subscription-events'].topic}",
        groupId = "#{kafkaConsumerProperties.consumers['subscription-events'].groupId}",
        containerFactory = "subscriptionEventsKafkaListenerContainerFactory")
public class SubscriptionInvalidatedEventHandler {

    private final SubscriptionCacheService subscriptionCacheService;

    @Transactional
    @KafkaHandler
    public void handleSubscriptionChanged(SubscriptionInvalidatedEvent event) {
        log.info("Инвалидация подписки в кеше у пользователя: {}", event.username());
        subscriptionCacheService.invalidateCache(event.username());
    }

}
