package com.hamamoto.shortifier.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamamoto.shortifier.dto.ShortenRequest;
import com.hamamoto.shortifier.repository.UrlMappingRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
                .andExpect(jsonPath("$.shortCode").value(hasLength(greaterThanOrEqualTo(7))))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/very-long-url"))
                .andExpect(jsonPath("$.shortUrl").value(matchesPattern("http://localhost:8080/[a-zA-Z0-9]+")))
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

    @Test
    void redirect_shouldReturn302AndRedirectToOriginalUrl() throws Exception {
        // Given - create a short URL first
        var request = new ShortenRequest("https://example.com/original", null);
        var createResponse = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var shortCode = objectMapper.readTree(createResponse).get("shortCode").asText();

        // When/Then - redirect to original URL
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/original"));
    }

    @Test
    void redirect_withNonExistentShortCode_shouldReturn404() throws Exception {
        // When/Then
        mockMvc.perform(get("/xxxxx"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Short URL not found")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void redirect_withExpiredUrl_shouldReturn410() throws Exception {
        // Given - create a short URL that already expired
        var expiredTime = LocalDateTime.now().minusDays(1);
        var request = new ShortenRequest("https://example.com/expired", expiredTime);
        var createResponse = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var shortCode = objectMapper.readTree(createResponse).get("shortCode").asText();

        // When/Then - should return 410 Gone
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.message").value(containsString("Short URL has expired")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void redirect_withFutureExpiresAt_shouldRedirect() throws Exception {
        // Given - create a short URL with future expiration
        var futureTime = LocalDateTime.now().plusDays(7);
        var request = new ShortenRequest("https://example.com/future", futureTime);
        var createResponse = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var shortCode = objectMapper.readTree(createResponse).get("shortCode").asText();

        // When/Then - should redirect normally
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/future"));
    }
}