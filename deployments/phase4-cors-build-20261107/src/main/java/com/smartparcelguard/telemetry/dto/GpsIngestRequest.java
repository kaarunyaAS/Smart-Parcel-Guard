package com.smartparcelguard.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.smartparcelguard.common.DataSource;
import com.smartparcelguard.tracking.GpsSignalStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record GpsIngestRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @NotNull @JsonAlias("altitude") @DecimalMin("-500.0") @DecimalMax("20000.0") BigDecimal altitudeMeters,
        @NotNull @JsonAlias("speed") @DecimalMin("0.0") @DecimalMax("1000.0") BigDecimal speedMps,
        @DecimalMin("0.0") BigDecimal accuracyMeters,
        @NotNull Instant recordedAt,
        @NotNull DataSource dataSource,
        GpsSignalStatus gpsSignalStatus
) {
}
