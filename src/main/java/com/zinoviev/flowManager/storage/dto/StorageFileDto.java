package com.zinoviev.flowManager.storage.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;

@Getter
@RequiredArgsConstructor
public class StorageFileDto {

    private final InputStream inputStream;

    private final String fileKey;

    private final String contentType;

    private final long contentLength;
}
