package com.smartparcelguard.telemetry.dto;

import com.smartparcelguard.camera.CameraRole;
import com.smartparcelguard.common.DataSource;

import java.time.Instant;

public record EvidenceResponse(
        Long id,
        Long parcelId,
        Long deviceId,
        CameraRole cameraRole,
        String fileName,
        String contentType,
        Long byteSize,
        String checksumSha256,
        Instant capturedAt,
        DataSource dataSource,
        String contentPath
) {
}
