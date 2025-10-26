package com.hamamoto.shortifier.controller;

import com.hamamoto.shortifier.dto.ShortenRequest;
import com.hamamoto.shortifier.dto.ShortenResponse;
import com.hamamoto.shortifier.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request) {
        log.info("Received shorten request for URL: {}", request.getUrl());
        var response = urlShortenerService.shortenUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public RedirectView redirect(@PathVariable String shortCode) {
        log.info("Received redirect request for short code: {}", shortCode);
        var originalUrl = urlShortenerService.getOriginalUrl(shortCode);
        return new RedirectView(originalUrl);
    }
}