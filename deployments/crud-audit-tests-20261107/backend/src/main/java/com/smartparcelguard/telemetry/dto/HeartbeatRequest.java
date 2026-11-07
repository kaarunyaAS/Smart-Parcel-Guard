package com.smartparcelguard.telemetry.dto;

import com.smartparcelguard.common.ComponentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record HeartbeatRequest(
        @Min(0) @Max(100) Integer batteryPercent,
        ComponentStatus gpsStatus,
        ComponentStatus sensorStatus,
        ComponentStatus cameraStatus
) {
}
