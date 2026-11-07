package com.smartparcelguard.device.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartparcelguard.common.ComponentStatus;
import com.smartparcelguard.device.DeviceStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record DeviceResponse(
        Long id,
        String deviceCode,
        String firmwareVersion,
        DeviceStatus status,
        boolean enabled,
        Integer batteryPercent,
        ComponentStatus gpsStatus,
        ComponentStatus sensorStatus,
        ComponentStatus cameraStatus,
        Instant lastSeenAt,
        Long assignedParcelId,
        String assignedParcelCode,
        boolean online,
        String deviceToken
) {
}
