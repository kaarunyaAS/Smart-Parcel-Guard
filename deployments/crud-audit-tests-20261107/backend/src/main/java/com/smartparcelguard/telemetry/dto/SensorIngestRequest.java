package com.smartparcelguard.telemetry.dto;

import com.smartparcelguard.common.DataSource;
import com.smartparcelguard.sensor.ReedSwitch;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record SensorIngestRequest(
        @NotNull BigDecimal accelerationX,
        @NotNull BigDecimal accelerationY,
        @NotNull BigDecimal accelerationZ,
        @NotNull BigDecimal gyroscopeX,
        @NotNull BigDecimal gyroscopeY,
        @NotNull BigDecimal gyroscopeZ,
        @NotNull ReedSwitch reedSwitch,
        @Min(0) Integer durationMs,
        @NotNull Instant recordedAt,
        @NotNull DataSource dataSource
) {
}
