package com.smartparcelguard.telemetry;

import com.smartparcelguard.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public final class GpsCoordinates {

    public static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);

    private GpsCoordinates() {
    }

    public static void validate(BigDecimal latitude,
                                BigDecimal longitude,
                                BigDecimal altitudeMeters,
                                BigDecimal speedMps,
                                Instant recordedAt,
                                Instant now) {
        requireRange(latitude, new BigDecimal("-90"), new BigDecimal("90"), "latitude");
        requireRange(longitude, new BigDecimal("-180"), new BigDecimal("180"), "longitude");
        requireRange(altitudeMeters, new BigDecimal("-500"), new BigDecimal("20000"), "altitudeMeters");
        requireRange(speedMps, BigDecimal.ZERO, new BigDecimal("1000"), "speedMps");
        if (recordedAt == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "recordedAt is required");
        }
        if (recordedAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "recordedAt cannot be more than five minutes in the future");
        }
    }

    private static void requireRange(BigDecimal value, BigDecimal min, BigDecimal max, String field) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, field + " is outside the accepted range");
        }
    }
}
