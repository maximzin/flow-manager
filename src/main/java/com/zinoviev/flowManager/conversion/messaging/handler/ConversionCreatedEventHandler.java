package com.zinoviev.flowManager.conversion.messaging.handler;

import com.zinoviev.flowManager.conversion.dao.ConversionTaskRepository;
import com.zinoviev.flowManager.conversion.messaging.event.ConversionProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
@KafkaListener(
        topics = "${topic.conversion.created.events}",
        groupId = "${spring.kafka.consumer.group-id}")
public class ConversionCreatedEventHandler {

    private final ConversionTaskRepository conversionTaskRepository;

    @Transactional
    @KafkaHandler
    public void handle(
            @Payload ConversionProcessedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {

        UUID uuidMessageKey = UUID.fromString(messageKey);
        log.info("Получено сообщение с messageKey: {}", uuidMessageKey);

        // Обновим запись с учетом идемпотентности (поля last_processed_event_id)
        conversionTaskRepository.updateTaskAfterConversion(
                uuidMessageKey,
                event.status(),
                event.convertedFileKey(),
                event.errorMessage(),
                event.eventId());


    }

}
