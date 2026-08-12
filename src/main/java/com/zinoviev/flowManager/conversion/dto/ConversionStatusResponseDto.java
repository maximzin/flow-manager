package com.zinoviev.flowManager.conversion.dto;

import com.zinoviev.flowManager.conversion.model.ConversionTask;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversionStatusResponseDto (

    UUID id,

    ConversionTask.TaskStatus status,

    LocalDateTime updatedAt

) {}
