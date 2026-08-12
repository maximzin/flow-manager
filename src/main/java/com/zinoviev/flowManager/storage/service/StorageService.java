package com.zinoviev.flowManager.storage.service;

import com.zinoviev.flowManager.storage.dto.StorageFileDto;

public interface StorageService {

    void uploadFile(String fileKey, byte[] fileBytes, String contentType);

    StorageFileDto getFile(String fileKey);

    boolean exists(String key);

}
