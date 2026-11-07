package com.smartparcelguard.telemetry.dto;

import com.smartparcelguard.common.DataSource;
import com.smartparcelguard.tracking.GpsSignalStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record LocationResponse(
        Long id,
        Long parcelId,
        Long deviceId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal altitudeMeters,
        BigDecimal speedMps,
        BigDecimal accuracyMeters,
        Instant recordedAt,
        DataSource dataSource,
        GpsSignalStatus gpsSignalStatus,
        BigDecimal[] latLng
) {
}
