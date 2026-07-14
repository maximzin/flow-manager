package com.zinoviev.flowManager.conversion.controller;

import com.zinoviev.flowManager.conversion.dto.ConversionStatusResponseDto;
import com.zinoviev.flowManager.conversion.dto.ConversionTaskResponseDto;
import com.zinoviev.flowManager.conversion.exception.FileDownloadException;
import com.zinoviev.flowManager.conversion.service.ConversionService;
import com.zinoviev.flowManager.core.util.FileKeyUtils;
import com.zinoviev.flowManager.storage.dto.StorageFileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversion")
@Slf4j
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;

    @PostMapping
    public ResponseEntity<ConversionTaskResponseDto> uploadFileForConversion(@RequestParam("file") MultipartFile fileFromUser) {
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
        StorageFileDto storageFileDto = conversionService.getConvertedFile(taskId);

        StreamingResponseBody stream = outputStream -> {
            try (InputStream in = storageFileDto.getInputStream()) {
                IOUtils.copy(in, outputStream);
            } catch (Exception e) {
                log.error("Ошибка передачи файла, id: {}", taskId, e);
                throw new FileDownloadException("Ошибка передачи файла");
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storageFileDto.getContentType()))
                .contentLength(storageFileDto.getContentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + FileKeyUtils.parseFileNameWithExtension(storageFileDto.getFileKey())
                                + "\"")
                .body(stream);
    }

}
