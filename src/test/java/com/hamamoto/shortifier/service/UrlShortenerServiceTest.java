package com.hamamoto.shortifier.service;

import com.hamamoto.shortifier.dto.ShortenRequest;
import com.hamamoto.shortifier.dto.ShortenResponse;
import com.hamamoto.shortifier.entity.UrlMapping;
import com.hamamoto.shortifier.exception.ShortUrlExpiredException;
import com.hamamoto.shortifier.exception.ShortUrlNotFoundException;
import com.hamamoto.shortifier.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlShortenerService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void shortenUrl_shouldGenerateShortCode() {
        // Given
        var request = new ShortenRequest("https://example.com/very-long-url", null);

        var savedMapping = UrlMapping.builder()
                .id(1L)
                .shortCode("abc12")
                .originalUrl(request.getUrl())
                .createdAt(LocalDateTime.now())
                .accessCount(0L)
                .build();

        when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // When
        var response = urlShortenerService.shortenUrl(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getShortCode()).isEqualTo("abc12");
        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com/very-long-url");
        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8080/abc12");
        assertThat(response.getCreatedAt()).isNotNull();

        verify(urlMappingRepository, times(1)).save(any(UrlMapping.class));
    }

    @Test
    void shortenUrl_withExpiresAt_shouldSetExpiration() {
        // Given
        var expiresAt = LocalDateTime.now().plusDays(7);
        var request = new ShortenRequest("https://example.com/test", expiresAt);

        var savedMapping = UrlMapping.builder()
                .id(1L)
                .shortCode("xyz99")
                .originalUrl(request.getUrl())
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .accessCount(0L)
                .build();

        when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // When
        var response = urlShortenerService.shortenUrl(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void shortenUrl_shouldRetryOnCollision() {
        // Given
        var request = new ShortenRequest("https://example.com/test", null);

        var savedMapping = UrlMapping.builder()
                .id(1L)
                .shortCode("valid")
                .originalUrl(request.getUrl())
                .createdAt(LocalDateTime.now())
                .accessCount(0L)
                .build();

        // Simulate 3 collisions, then success
        when(urlMappingRepository.existsByShortCode(anyString()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);
        when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // When
        var response = urlShortenerService.shortenUrl(request);

        // Then
        assertThat(response).isNotNull();
        verify(urlMappingRepository, times(4)).existsByShortCode(anyString());
        verify(urlMappingRepository, times(1)).save(any(UrlMapping.class));
    }

    @Test
    void shortenUrl_shouldThrowExceptionAfterMaxRetries() {
        // Given
        var request = new ShortenRequest("https://example.com/test", null);

        // Simulate all collisions
        when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> urlShortenerService.shortenUrl(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to generate unique short code");

        verify(urlMappingRepository, never()).save(any(UrlMapping.class));
    }

    @Test
    void generateShortCode_shouldBeExactly5Characters() {
        // Given
        var request = new ShortenRequest("https://example.com/test", null);

        var savedMapping = UrlMapping.builder()
                .id(1L)
                .shortCode("a1B2c")
                .originalUrl(request.getUrl())
                .createdAt(LocalDateTime.now())
                .accessCount(0L)
                .build();

        when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        // When
        var response = urlShortenerService.shortenUrl(request);

        // Then
        assertThat(response.getShortCode()).hasSize(5);
    }

    @Test
    void getOriginalUrl_shouldReturnUrl() {
        // Given
        var shortCode = "abc12";
        var urlMapping = UrlMapping.builder()
                .id(1L)
                .shortCode(shortCode)
                .originalUrl("https://example.com/test")
                .createdAt(LocalDateTime.now())
                .accessCount(0L)
                .build();

        when(urlMappingRepository.findByShortCode(shortCode)).thenReturn(Optional.of(urlMapping));

        // When
        var originalUrl = urlShortenerService.getOriginalUrl(shortCode);

        // Then
        assertThat(originalUrl).isEqualTo("https://example.com/test");
        verify(urlMappingRepository, times(1)).findByShortCode(shortCode);
    }

    @Test
    void getOriginalUrl_shouldThrowNotFoundException() {
        // Given
        var shortCode = "xyz99";
        when(urlMappingRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> urlShortenerService.getOriginalUrl(shortCode))
                .isInstanceOf(ShortUrlNotFoundException.class)
                .hasMessageContaining("Short URL not found: xyz99");
    }

    @Test
    void getOriginalUrl_shouldThrowExpiredException() {
        // Given
        var shortCode = "abc12";
        var urlMapping = UrlMapping.builder()
                .id(1L)
                .shortCode(shortCode)
                .originalUrl("https://example.com/test")
                .createdAt(LocalDateTime.now().minusDays(10))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .accessCount(0L)
                .build();

        when(urlMappingRepository.findByShortCode(shortCode)).thenReturn(Optional.of(urlMapping));

        // When/Then
        assertThatThrownBy(() -> urlShortenerService.getOriginalUrl(shortCode))
                .isInstanceOf(ShortUrlExpiredException.class)
                .hasMessageContaining("Short URL has expired: abc12");
    }

    @Test
    void getOriginalUrl_withNullExpiresAt_shouldReturnUrl() {
        // Given
        var shortCode = "abc12";
        var urlMapping = UrlMapping.builder()
                .id(1L)
                .shortCode(shortCode)
                .originalUrl("https://example.com/test")
                .createdAt(LocalDateTime.now())
                .expiresAt(null)
                .accessCount(0L)
                .build();

        when(urlMappingRepository.findByShortCode(shortCode)).thenReturn(Optional.of(urlMapping));

        // When
        var originalUrl = urlShortenerService.getOriginalUrl(shortCode);

        // Then
        assertThat(originalUrl).isEqualTo("https://example.com/test");
    }

    @Test
    void getOriginalUrl_withFutureExpiresAt_shouldReturnUrl() {
        // Given
        var shortCode = "abc12";
        var urlMapping = UrlMapping.builder()
                .id(1L)
                .shortCode(shortCode)
                .originalUrl("https://example.com/test")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .accessCount(0L)
                .build();

        when(urlMappingRepository.findByShortCode(shortCode)).thenReturn(Optional.of(urlMapping));

        // When
        var originalUrl = urlShortenerService.getOriginalUrl(shortCode);

        // Then
        assertThat(originalUrl).isEqualTo("https://example.com/test");
    }
}