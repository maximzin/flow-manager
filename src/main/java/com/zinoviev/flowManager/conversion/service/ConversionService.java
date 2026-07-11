package com.zinoviev.flowManager.conversion.service;

import com.zinoviev.flowManager.conversion.dto.ConversionTaskResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface ConversionService {

    ConversionTaskResponseDto processFileFromUser(MultipartFile file) throws IOException;

    void sendTasksToKafka() throws ExecutionException, InterruptedException;

}
