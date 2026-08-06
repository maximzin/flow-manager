package com.zinoviev.flowManager.conversion.cache.service;

import com.zinoviev.flowManager.conversion.api.SubscriptionServiceClient;
import com.zinoviev.flowManager.conversion.dto.CachedSubscriptionCheck;
import com.zinoviev.flowManager.conversion.dto.SubscriptionCheckRequestDto;
import com.zinoviev.flowManager.conversion.dto.SubscriptionCheckResultDto;
import com.zinoviev.flowManager.conversion.exception.SubscriptionServiceUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionCacheServiceImpl implements SubscriptionCacheService {

    private final SubscriptionServiceClient subscriptionServiceClient;
    private final RedisTemplate<String, CachedSubscriptionCheck> redisTemplate;

    @Value("${app.cache.subscription.ttl-minutes}")
    private int ttlMinutes;

    private static final String CACHE_KEY_PREFIX = "subscription:check:";

    @Override
    public SubscriptionCheckResultDto checkSubscription(String userLogin, long fileSizeBytes) {
        String cacheKey = CACHE_KEY_PREFIX + userLogin;

        // 1. Проверяем кеш
        CachedSubscriptionCheck cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("Проверка кеша подписки пользователя: {}", userLogin);

            // Проверяем, что файл всё ещё подходит под кешированный результат
            if (cached.allowed() && fileSizeBytes <= cached.maxFileSizeBytes()) {
                return mapToResponse(cached);
            }
            // Если файл больше кешированного лимита - идём в сервис
            if (!cached.allowed() && fileSizeBytes <= cached.maxFileSizeBytes()) {
                return mapToResponse(cached);
            }
            log.info("Cache result not applicable for file size {}, fetching fresh", fileSizeBytes);
        }

        // 2. Кеша нет или не подходит идём в subscription-service
        log.info("Отсутствует кеш подпски для пользователя: {}", userLogin);
        SubscriptionCheckResultDto response;
        try {
            response = subscriptionServiceClient.checkSubscription(
                    userLogin,
                    new SubscriptionCheckRequestDto(fileSizeBytes));
        } catch (FeignException e) {
            log.error("Сервис подписок недоступен", e);
            throw new SubscriptionServiceUnavailableException("Сервис подписок недоступен");
        }

        // 3. Кешируем результат
        CachedSubscriptionCheck cacheEntry = new CachedSubscriptionCheck(
                userLogin,
                response.allowed(),
                response.subscriptionTypeName(),
                response.maxFileSizeBytes(),
                response.expiresAt(),
                Instant.now());

        redisTemplate.opsForValue().set(cacheKey, cacheEntry, Duration.ofMinutes(ttlMinutes));
        log.info("Кеширую запись о подписке для пользователя: {}, с TTL: {} минут", userLogin, ttlMinutes);

        return response;
    }

    @Override
    public void invalidateCache(String userLogin) {
        String cacheKey = CACHE_KEY_PREFIX + userLogin;
        redisTemplate.delete(cacheKey);
        log.info("Инвалидация кеша подписки для пользователя: {}", userLogin);
    }

    private SubscriptionCheckResultDto mapToResponse(CachedSubscriptionCheck cached) {
        return new SubscriptionCheckResultDto(
                cached.userLogin(),
                cached.allowed(),
                cached.subscriptionType(),
                cached.maxFileSizeBytes(),
                cached.expiresAt(),
                null
        );
    }
}
