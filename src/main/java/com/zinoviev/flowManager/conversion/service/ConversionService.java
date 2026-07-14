package com.zinoviev.flowManager.conversion.service;

import com.zinoviev.flowManager.conversion.dto.ConversionStatusResponseDto;
import com.zinoviev.flowManager.conversion.dto.ConversionTaskResponseDto;
import com.zinoviev.flowManager.storage.dto.StorageFileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

public interface ConversionService {

    ConversionTaskResponseDto processFileFromUser(MultipartFile file);

    void sendTasksToKafka() throws ExecutionException, InterruptedException;

    ConversionStatusResponseDto getStatusOfConversionTask(UUID taskId);

    StorageFileDto getConvertedFile(UUID taskId);

    void cleanupUploadingTasks();

}
