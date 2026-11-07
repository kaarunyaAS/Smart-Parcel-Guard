package com.smartparcelguard.telemetry;

import com.smartparcelguard.sensor.MovementStatus;

import java.math.BigDecimal;

public record SensorAssessment(
        BigDecimal magnitude,
        MovementStatus movementStatus,
        boolean impactDetected,
        boolean vibrationOnly,
        boolean unauthorizedOpening
) {
}
