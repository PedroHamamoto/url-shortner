package com.hamamoto.shortifier.service;

import com.hamamoto.shortifier.component.ShortCodeGenerator;
import com.hamamoto.shortifier.dto.ShortenRequest;
import com.hamamoto.shortifier.dto.ShortenResponse;
import com.hamamoto.shortifier.entity.UrlMapping;
import com.hamamoto.shortifier.exception.ShortUrlExpiredException;
import com.hamamoto.shortifier.exception.ShortUrlNotFoundException;
import com.hamamoto.shortifier.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final RedisCounterService redisCounterService;
    private final ShortCodeGenerator shortCodeGenerator;

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
        var nextId = redisCounterService.getNextId();
        var shortCode = shortCodeGenerator.generate(nextId);
        log.debug("Generated short code: {} from ID: {}", shortCode, nextId);
        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        var urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        if (urlMapping.getExpiresAt() != null && urlMapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ShortUrlExpiredException(shortCode);
        }

        log.info("Redirecting short code {} to {}", shortCode, urlMapping.getOriginalUrl());
        return urlMapping.getOriginalUrl();
    }
}