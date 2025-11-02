package com.hamamoto.shortifier.component;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generates short, non-sequential codes using Hashids.
 * The salt parameter ensures that sequential IDs produce different codes
 * across different applications, preventing attackers from inferring the next short URL.
 */
@Component
public class ShortCodeGenerator {
    private final Hashids hashids;
    private static final int MIN_CODE_LENGTH = 7;

    public ShortCodeGenerator(@Value("${shortifier.salt:default-salt-change-in-production}") String salt) {
        this.hashids = new Hashids(salt, MIN_CODE_LENGTH);
    }

    /**
     * Generates a short code from a sequential ID.
     *
     * @param id the sequential ID from Redis counter
     * @return a short, obfuscated code
     */
    public String generate(long id) {
        return hashids.encode(id);
    }

    /**
     * Decodes a short code back to its original ID.
     * Useful for retrieving the original ID from a short code.
     *
     * @param code the short code
     * @return the original sequential ID
     */
    public long decode(String code) {
        var decoded = hashids.decode(code);
        return decoded.length > 0 ? decoded[0] : -1L;
    }
}