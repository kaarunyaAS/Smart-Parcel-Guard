package com.smartparcelguard.telemetry.dto;

import com.smartparcelguard.common.ComponentStatus;
import com.smartparcelguard.device.DeviceStatus;

import java.time.Instant;

public record DevicePresenceResponse(
        Long deviceId,
        String deviceCode,
        Long parcelId,
        DeviceStatus status,
        boolean online,
        Instant lastSeenAt,
        int offlineAfterSeconds,
        Integer batteryPercent,
        ComponentStatus gpsStatus,
        ComponentStatus sensorStatus,
        ComponentStatus cameraStatus
) {
}
