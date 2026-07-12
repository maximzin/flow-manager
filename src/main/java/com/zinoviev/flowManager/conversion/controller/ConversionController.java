package com.zinoviev.flowManager.conversion.controller;

import com.zinoviev.flowManager.conversion.dao.ConversionTaskRepository;
import com.zinoviev.flowManager.conversion.dto.ConversionStatusResponseDto;
import com.zinoviev.flowManager.conversion.dto.ConversionTaskResponseDto;
import com.zinoviev.flowManager.conversion.model.ConversionTask;
import com.zinoviev.flowManager.conversion.service.ConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversion")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;

    @PostMapping
    public ResponseEntity<ConversionTaskResponseDto> uploadFileForConversion(@RequestParam("file") MultipartFile fileFromUser) throws IOException {
        ConversionTaskResponseDto responseDto = conversionService.processFileFromUser(fileFromUser);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(responseDto.id())
                .toUri();
        return ResponseEntity.created(location).body(responseDto);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ConversionStatusResponseDto> getStatusOfConversion(@PathVariable("taskId") UUID taskId) {
        ConversionStatusResponseDto responseDto = conversionService.getStatusOfConversionTask(taskId);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{taskId}/file")
    public ResponseEntity<StreamingResponseBody> getConvertedFile(@PathVariable UUID taskId) {
        StreamingResponseBody streamingResponseBody = conversionService.getConvertedFile(taskId);
    }

}
