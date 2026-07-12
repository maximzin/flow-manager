package com.zinoviev.flowManager.conversion.service;

import com.zinoviev.flowManager.conversion.dao.ConversionTaskRepository;
import com.zinoviev.flowManager.conversion.dto.ConversionStatusResponseDto;
import com.zinoviev.flowManager.conversion.dto.ConversionTaskResponseDto;
import com.zinoviev.flowManager.conversion.exception.ConversionProcessingException;
import com.zinoviev.flowManager.conversion.exception.ConversionTaskNotFoundException;
import com.zinoviev.flowManager.conversion.exception.FileNotConvertedException;
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
import software.amazon.awssdk.services.s3.S3Client;

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

    private final StorageService storageService;
    private final ConversionTaskRepository conversionTaskRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    @Override
    public ConversionTaskResponseDto processFileFromUser(MultipartFile file) throws IOException {
        // Создаем в MinIO копию файла
        String originalFileKey = String.join("", originalFilesDir, file.getOriginalFilename());
        storageService.uploadFile(originalFileKey, file.getBytes(), file.getContentType());

        // Делаем запись в БД
        UUID taskId = UUID.randomUUID();
        ConversionTask newConversionTask = new ConversionTask(taskId, originalFileKey);
        conversionTaskRepository.save(newConversionTask);

        return new ConversionTaskResponseDto(
                taskId,
                ConversionTask.TaskStatus.PENDING,
                LocalDateTime.now()
        );
    }

    @Transactional
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

            conversionTaskRepository.updateStatusAfterSending(ConversionTask.TaskStatus.PENDING, LocalDateTime.now());
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
}
