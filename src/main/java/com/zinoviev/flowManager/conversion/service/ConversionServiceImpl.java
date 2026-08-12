package com.zinoviev.flowManager.conversion.service;

import com.zinoviev.flowManager.conversion.api.SubscriptionServiceClient;
import com.zinoviev.flowManager.conversion.cache.service.SubscriptionCacheService;
import com.zinoviev.flowManager.conversion.dao.ConversionTaskRepository;
import com.zinoviev.flowManager.conversion.dto.ConversionStatusResponseDto;
import com.zinoviev.flowManager.conversion.dto.ConversionTaskResponseDto;
import com.zinoviev.flowManager.conversion.dto.SubscriptionCheckResultDto;
import com.zinoviev.flowManager.conversion.exception.*;
import com.zinoviev.flowManager.conversion.metrics.SubscriptionMetrics;
import com.zinoviev.flowManager.conversion.model.ConversionTask;
import com.zinoviev.flowManager.conversion.messaging.event.ConversionCreatedEvent;
import com.zinoviev.flowManager.storage.dto.StorageFileDto;
import com.zinoviev.flowManager.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversionServiceImpl implements ConversionService {

    @Value("${storage.directory.conversion.original}")
    private String originalFilesDir;

    @Value("${topic.conversion.created.events}")
    private String conversionCreatedEventsTopicName;

    @Value("${conversion.uploading-tasks.duration-minutes-to-cleanup}")
    private Integer durationMinutesToCleanupUploadingTasks;

    private final StorageService storageService;
    private final ConversionTaskRepository conversionTaskRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SubscriptionServiceClient subscriptionServiceClient;
    private final SubscriptionCacheService subscriptionCacheService;
    private final SubscriptionMetrics subscriptionMetrics;

    @Override
    public ConversionTaskResponseDto processFileFromUser(String userLogin, MultipartFile file) {

        // Проверка наличия файла
        if (file.getSize() <= 0) throw new EmptyFileUploadException("Пришёл пустой файл");

        // Проверка подписки
        SubscriptionCheckResultDto checkResult = subscriptionCacheService.checkSubscription(userLogin, file.getSize());
        if (!checkResult.allowed()) {
            log.info("Ошибка подписки у пользователя: {}, сообщение: {}", userLogin, checkResult.message());
            throw new SubscriptionTypeException(checkResult.message());
        }
        log.info("Подписка позволяет пользователю: {} загрузить файл", userLogin);

        // 1. Создаём запись в БД
        UUID taskId = UUID.randomUUID();
        String originalFileKey = originalFilesDir + taskId + "/" + file.getOriginalFilename();

        ConversionTask task = new ConversionTask(taskId, originalFileKey);
        task.setStatus(ConversionTask.TaskStatus.UPLOADING);
        task.setOutboxSent(false);
        conversionTaskRepository.save(task);

        // 2. Загружаем файл в MinIO
        try {
            storageService.uploadFile(originalFileKey, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            log.error("Ошибка загрузки файла {}, taskId: {}", originalFileKey, taskId, e);
            throw new FileUploadException("Не удалось сохранить файл");
        }

        // 3. Обновляем запись
        ConversionTask managedTask = conversionTaskRepository.findById(taskId)
                .orElseThrow(() -> new ConversionTaskNotFoundException("Задача не найдена после загрузки"));
        managedTask.setStatus(ConversionTask.TaskStatus.UPLOADED);
        conversionTaskRepository.save(managedTask);

        subscriptionMetrics.recordProcessedTask(checkResult.subscriptionTypeName());

        return new ConversionTaskResponseDto(
                managedTask.getId(),
                managedTask.getStatus(),
                managedTask.getCreatedAt()
        );
    }

    @Override
    public void sendTasksToKafka() throws ExecutionException, InterruptedException {
        List<ConversionTask> newTasks = conversionTaskRepository.findAllByStatusEqualsAndOutboxSentEquals(ConversionTask.TaskStatus.UPLOADED, false);

        for (ConversionTask task : newTasks) {
            ConversionCreatedEvent conversionCreatedEvent = new ConversionCreatedEvent(
                    UUID.randomUUID(),
                    task.getOriginalFileKey(),
                    LocalDateTime.now()
            );

            ProducerRecord<String, Object> record =
                    new ProducerRecord<>(
                            conversionCreatedEventsTopicName,
                            task.getId().toString(),
                            conversionCreatedEvent
                    );

            kafkaTemplate.send(record).get();
            log.info("Сообщение в {} успешно отправлено, conversion_task_id: {}", conversionCreatedEventsTopicName, task.getId());

            task.setStatus(ConversionTask.TaskStatus.PENDING);
            task.setOutboxSent(true);
            conversionTaskRepository.save(task);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ConversionStatusResponseDto getStatusOfConversionTask(UUID id) {
        ConversionTask task = conversionTaskRepository.findById(id)
                .orElseThrow(() -> new ConversionTaskNotFoundException("Запись не найдена"));

        return new ConversionStatusResponseDto(
                id,
                task.getStatus(),
                task.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    @Override
    public StorageFileDto getConvertedFile(UUID taskId) {
        ConversionTask task = conversionTaskRepository.findById(taskId)
                .orElseThrow(() -> new ConversionTaskNotFoundException("Запись не найдена"));

        if (task.getStatus() != ConversionTask.TaskStatus.COMPLETED) {
            if (task.getStatus() == ConversionTask.TaskStatus.FAILED) {
                log.error("Файл не удалось сконвертировать, id: {}", taskId);
                throw new FileNotConvertedException("Файл не удалось сконвертировать");
            } else {
                log.error("Задача конвертации еще в процессе, id: {}", taskId);
                throw new ConversionProcessingException("Задача конвертации еще в процессе");
            }
        }

        String fileKey = task.getConvertedFileKey();
        if (fileKey == null) {
            throw new FileNotConvertedException("Файл не удалось сконвертировать");
        }

        return storageService.getFile(fileKey);
    }

    @Transactional
    @Override
    public void cleanupUploadingTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(durationMinutesToCleanupUploadingTasks);
        List<ConversionTask> staleTasks = conversionTaskRepository
                .findByStatusAndCreatedAtBefore(ConversionTask.TaskStatus.UPLOADING, threshold);

        for (ConversionTask task : staleTasks) {
            boolean fileExists = storageService.exists(task.getOriginalFileKey());
            if (fileExists) {
                task.setStatus(ConversionTask.TaskStatus.UPLOADED);
                conversionTaskRepository.save(task);
                log.info("Задача taskId: {} восстановлена: файл найден, статус обновлён на UPLOADED", task.getId());
            } else {
                task.setStatus(ConversionTask.TaskStatus.FAILED);
                task.setErrorMessage("Файл не был загружен, задача удалена по таймауту");
                conversionTaskRepository.save(task);
                log.warn("Задача taskId: {} помечена как FAILED: файл отсутствует", task.getId());
            }
        }
    }
}
