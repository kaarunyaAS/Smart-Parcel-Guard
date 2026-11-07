package com.smartparcelguard.common;

import com.smartparcelguard.exception.ApiException;
import org.springframework.http.HttpStatus;

public final class TextSanitizer {

    private TextSanitizer() {
    }

    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.chars().anyMatch(ch -> ch < 32)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Input contains unsupported control characters");
        }
        return trimmed;
    }

    public static String required(String value, String field) {
        String cleaned = clean(value);
        if (cleaned == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return cleaned;
    }

    public static String lower(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase();
    }
}
