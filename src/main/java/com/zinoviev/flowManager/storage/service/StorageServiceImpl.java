package com.zinoviev.flowManager.storage.service;

import com.zinoviev.flowManager.storage.dto.StorageFileDto;
import com.zinoviev.flowManager.storage.exception.FileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    @Value("${minio.bucket-name}")
    private String bucketName;

    private final S3Client s3Client;

    @Transactional
    @Override
    public void uploadFile(String fileKey, byte[] fileBytes, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(fileBytes));
        log.info("В хранилище загружен файл: {}", fileKey);
    }

    @Transactional(readOnly = true)
    @Override
    public StorageFileDto getFile(String fileKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        ResponseInputStream<GetObjectResponse> s3Object;
        try {
            s3Object = s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            log.error("Файл не найден, fileKey: {}", fileKey, e);
            throw new FileNotFoundException("Файл не найден");
        }

        GetObjectResponse metadata = s3Object.response();
        String contentType = metadata.contentType() != null ? metadata.contentType() : "application/octet-stream";
        long contentLength = metadata.contentLength();

        return new StorageFileDto(s3Object, fileKey, contentType, contentLength);
    }

    @Override
    public boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Ошибка при проверке существования объекта, key: {}", key, e);
            return false;
        }
    }
}
