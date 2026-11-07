package com.smartparcelguard.telemetry.dto;

import com.smartparcelguard.common.DataSource;
import com.smartparcelguard.sensor.MovementStatus;
import com.smartparcelguard.sensor.ReedSwitch;

import java.math.BigDecimal;
import java.time.Instant;

public record SensorReadingResponse(
        Long id,
        Long parcelId,
        Long deviceId,
        BigDecimal accelerationX,
        BigDecimal accelerationY,
        BigDecimal accelerationZ,
        BigDecimal gyroscopeX,
        BigDecimal gyroscopeY,
        BigDecimal gyroscopeZ,
        BigDecimal magnitude,
        ReedSwitch reedSwitch,
        MovementStatus movementStatus,
        boolean impactDetected,
        boolean unauthorizedOpening,
        Instant recordedAt,
        DataSource dataSource
) {
}
