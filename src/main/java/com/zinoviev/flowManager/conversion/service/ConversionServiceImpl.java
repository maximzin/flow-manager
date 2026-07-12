package com.zinoviev.flowManager.conversion.service;

import com.zinoviev.flowManager.conversion.dao.ConversionTaskRepository;
import com.zinoviev.flowManager.conversion.dto.ConversionStatusResponseDto;
import com.zinoviev.flowManager.conversion.dto.ConversionTaskResponseDto;
import com.zinoviev.flowManager.conversion.exception.ConversionTaskNotFoundException;
import com.zinoviev.flowManager.conversion.messaging.event.ConversionEventStatus;
import com.zinoviev.flowManager.conversion.model.ConversionTask;
import com.zinoviev.flowManager.conversion.messaging.event.ConversionCreatedEvent;
import com.zinoviev.flowManager.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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

        ConversionTaskResponseDto responseDto = new ConversionTaskResponseDto(
                taskId,
                ConversionTask.TaskStatus.PENDING,
                LocalDateTime.now()
        );
        return responseDto;
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
                .orElseThrow(() -> new ConversionTaskNotFoundException(
                        String.format("Запись задачи с id: %s не найдена", id)));

        return new ConversionStatusResponseDto(
                id,
                task.getStatus(),
                task.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    @Override
    public StreamingResponseBody getConvertedFileUU(UUID taskId) {
        ConversionTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Задача не найдена"));

        if (task.getStatus() != ConversionTask.TaskStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Файл ещё не готов или обработка завершилась с ошибкой");
        }

        String fileKey = task.getConvertedFileKey();
        if (fileKey == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Отсутствует ссылка на сконвертированный файл");
        }

        // Получаем метаданные объекта, чтобы узнать размер и content-type
        StatObjectResponse stat;
        try {
            stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(fileKey)
                            .build()
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка доступа к файлу в хранилище", e);
        }

        String contentType = stat.contentType() != null ? stat.contentType() : "application/octet-stream";
        long contentLength = stat.size();

        // Потоковая передача файла
        StreamingResponseBody stream = outputStream -> {
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(fileKey)
                            .build()
            )) {
                IOUtils.copy(inputStream, outputStream);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка при чтении файла", e);
            }
        };
    }
}
