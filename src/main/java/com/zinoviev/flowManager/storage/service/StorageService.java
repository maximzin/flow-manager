package com.zinoviev.flowManager.storage.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    void uploadFile(String fileKey, byte[] fileBytes, String contentType);

}
