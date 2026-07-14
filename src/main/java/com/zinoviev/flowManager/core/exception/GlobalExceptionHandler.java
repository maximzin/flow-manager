package com.zinoviev.flowManager.core.exception;

import com.zinoviev.flowManager.core.dto.ExceptionDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ExceptionDto> handleNotFound(ResourceNotFoundException ex) {
        ExceptionDto body = new ExceptionDto(
                HttpStatus.NOT_FOUND.value(),
                "Ресурс не найден",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
    
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ExceptionDto> handleBusiness(BusinessException ex) {
        ExceptionDto body = new ExceptionDto(
                HttpStatus.CONFLICT.value(),
                "Ошибка бизнес-логики",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ProcessingException.class)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<ExceptionDto> handleProcessing(ProcessingException ex) {
        ExceptionDto body = new ExceptionDto(
                HttpStatus.ACCEPTED.value(),
                "Операция находится в процессе",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @ExceptionHandler(DownloadException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ExceptionDto> handleDownloadException(DownloadException ex) {
        ExceptionDto body = new ExceptionDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ошибка скачивания файла",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(UploadException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ExceptionDto> handleUploadException(UploadException ex) {
        ExceptionDto body = new ExceptionDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ошибка загрузки файла",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
