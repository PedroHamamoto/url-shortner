package com.hamamoto.shortifier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamamoto.shortifier.dto.ShortenRequest;
import com.hamamoto.shortifier.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UrlShortenerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @BeforeEach
    void setUp() {
        urlMappingRepository.deleteAll();
    }

    @Test
    void shortenUrl_shouldReturn201WithShortCode() throws Exception {
        // Given
        var request = new ShortenRequest("https://example.com/very-long-url", null);

        // When/Then
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.shortCode").value(hasLength(5)))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/very-long-url"))
                .andExpect(jsonPath("$.shortUrl").value(matchesPattern("http://localhost:8080/[a-zA-Z0-9]{5}")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.expiresAt").doesNotExist());

        // Verify database
        assertThat(urlMappingRepository.count()).isEqualTo(1);
    }

    @Test
    void shortenUrl_withExpiresAt_shouldIncludeExpiration() throws Exception {
        // Given
        var expiresAt = LocalDateTime.now().plusDays(7);
        var request = new ShortenRequest("https://example.com/test", expiresAt);

        // When/Then
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void shortenUrl_withBlankUrl_shouldReturn400() throws Exception {
        // Given
        var request = new ShortenRequest("", null);

        // When/Then
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.url").exists());

        // Verify database
        assertThat(urlMappingRepository.count()).isZero();
    }

    @Test
    void shortenUrl_withInvalidUrlFormat_shouldReturn400() throws Exception {
        // Given
        var request = new ShortenRequest("not-a-valid-url", null);

        // When/Then
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.url").value("URL must start with http:// or https://"));

        // Verify database
        assertThat(urlMappingRepository.count()).isZero();
    }

    @Test
    void shortenUrl_withUrlTooLong_shouldReturn400() throws Exception {
        // Given
        var longUrl = "https://example.com/" + "a".repeat(2050);
        var request = new ShortenRequest(longUrl, null);

        // When/Then
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.url").value("URL cannot exceed 255 characters"));

        // Verify database
        assertThat(urlMappingRepository.count()).isZero();
    }

    @Test
    void shortenUrl_multipleCalls_shouldGenerateDifferentShortCodes() throws Exception {
        // Given
        var request = new ShortenRequest("https://example.com/same-url", null);

        // When - create two short URLs for the same original URL
        var response1 = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var response2 = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then - verify different short codes
        var shortCode1 = objectMapper.readTree(response1).get("shortCode").asText();
        var shortCode2 = objectMapper.readTree(response2).get("shortCode").asText();

        assertThat(shortCode1).isNotEqualTo(shortCode2);
        assertThat(urlMappingRepository.count()).isEqualTo(2);
    }
}