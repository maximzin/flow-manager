package com.zinoviev.flowManager.conversion.messaging.handler;

import com.zinoviev.flowManager.conversion.dao.ConversionTaskRepository;
import com.zinoviev.flowManager.conversion.exception.ConversionTaskNotFoundException;
import com.zinoviev.flowManager.conversion.messaging.event.ConversionEventStatus;
import com.zinoviev.flowManager.conversion.messaging.event.ConversionProcessedEvent;
import com.zinoviev.flowManager.conversion.model.ConversionTask;
import com.zinoviev.flowManager.core.exception.UnknownMessageStatusException;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
@KafkaListener(
        topics = "${topic.conversion.processed.events}",
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
        ConversionTask task = conversionTaskRepository.findById(uuidMessageKey)
                .orElseThrow(() -> new ConversionTaskNotFoundException(
                        String.format("Запись задачи с id: %s не найдена", uuidMessageKey)));

        // Проверяем на идемпотентность через eventId
        if (task.getLastProcessedEventId() != null
                && task.getLastProcessedEventId().equals(event.eventId())) {
            log.warn("Событие с messageKey: {} уже было обработано, eventId: {}", uuidMessageKey, event.eventId());
            return;
        }

        switch (event.status()) {
            case ConversionEventStatus.COMPLETED: {
                task.setStatus(ConversionTask.TaskStatus.COMPLETED);
                break;
            }
            case ConversionEventStatus.FAILED: {
                task.setStatus(ConversionTask.TaskStatus.FAILED);
                task.setErrorMessage(event.errorMessage());
                break;
            }
            default: {
                log.error("Получен неизвестный статус сообщения, id: {}, статус: {}, статус неизвествен", uuidMessageKey, event.status().name());
                throw new UnknownMessageStatusException("Неизвестный статус сообщения в таблице Inbox");
            }
        }

        task.setConvertedFileKey(event.convertedFileKey());
        task.setLastProcessedEventId(event.eventId());
        task.setUpdatedAt(LocalDateTime.now());

        conversionTaskRepository.save(task);
        log.info("Событие с messageKey: {} было успешно обработано", uuidMessageKey);
    }

}
