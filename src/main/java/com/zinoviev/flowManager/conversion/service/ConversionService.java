package com.zinoviev.flowManager.conversion.service;

import com.zinoviev.flowManager.conversion.dto.ConversionStatusResponseDto;
import com.zinoviev.flowManager.conversion.dto.ConversionTaskResponseDto;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public interface ConversionService {

    ConversionTaskResponseDto processFileFromUser(MultipartFile file) throws IOException;

    void sendTasksToKafka() throws ExecutionException, InterruptedException;

    ConversionStatusResponseDto getStatusOfConversionTask(UUID taskId);

    StreamingResponseBody getConvertedFileUU(UUID taskId);

}
