package com.hamamoto.shortifier.exception;

public class ShortUrlExpiredException extends RuntimeException {

    public ShortUrlExpiredException(String shortCode) {
        super("Short URL has expired: " + shortCode);
    }
}