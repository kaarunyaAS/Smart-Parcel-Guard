package com.smartparcelguard.device.dto;

import com.smartparcelguard.device.DeviceStatus;
import jakarta.validation.constraints.Size;

public record DeviceUpdateRequest(
        @Size(max = 40) String firmwareVersion,
        Boolean enabled,
        DeviceStatus status
) {
}
