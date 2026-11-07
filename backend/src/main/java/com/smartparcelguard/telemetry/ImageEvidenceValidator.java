package com.smartparcelguard.telemetry;

import com.smartparcelguard.exception.ApiException;
import org.springframework.http.HttpStatus;

public final class ImageEvidenceValidator {

    public static final int MAX_BYTES = 8 * 1024 * 1024;

    private ImageEvidenceValidator() {
    }

    public static DetectedImage validate(byte[] bytes, String declaredContentType) {
        if (bytes == null || bytes.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image file is empty");
        }
        if (bytes.length > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Image exceeds the 8 MB limit");
        }
        DetectedImage detected = sniff(bytes);
        if (detected == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPEG and PNG images are accepted");
        }
        if (declaredContentType == null || !detected.contentType().equalsIgnoreCase(declaredContentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image content type does not match the file");
        }
        return detected;
    }

    private static DetectedImage sniff(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return new DetectedImage("image/png", "png");
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return new DetectedImage("image/jpeg", "jpg");
        }
        return null;
    }

    public record DetectedImage(String contentType, String extension) {
    }
}
