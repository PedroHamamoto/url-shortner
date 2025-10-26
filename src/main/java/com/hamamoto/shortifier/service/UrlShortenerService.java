package com.hamamoto.shortifier.service;

import com.hamamoto.shortifier.dto.ShortenRequest;
import com.hamamoto.shortifier.dto.ShortenResponse;
import com.hamamoto.shortifier.entity.UrlMapping;
import com.hamamoto.shortifier.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SHORT_CODE_LENGTH = 5;
    private static final int MAX_RETRIES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UrlMappingRepository urlMappingRepository;

    @Value("${shortifier.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request) {
        var shortCode = generateUniqueShortCode();

        var urlMapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(request.getUrl())
                .expiresAt(request.getExpiresAt())
                .build();

        var saved = urlMappingRepository.save(urlMapping);

        log.info("Created short URL: {} -> {}", shortCode, request.getUrl());

        return ShortenResponse.builder()
                .shortCode(saved.getShortCode())
                .originalUrl(saved.getOriginalUrl())
                .shortUrl(baseUrl + "/" + saved.getShortCode())
                .createdAt(saved.getCreatedAt())
                .expiresAt(saved.getExpiresAt())
                .build();
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            var shortCode = generateRandomShortCode();
            if (!urlMappingRepository.existsByShortCode(shortCode)) {
                return shortCode;
            }
            log.warn("Short code collision detected: {}. Retry attempt {}/{}", shortCode, attempt + 1, MAX_RETRIES);
        }
        throw new IllegalStateException("Failed to generate unique short code after " + MAX_RETRIES + " attempts");
    }

    private String generateRandomShortCode() {
        var sb = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            var index = RANDOM.nextInt(BASE62_CHARS.length());
            sb.append(BASE62_CHARS.charAt(index));
        }
        return sb.toString();
    }
}