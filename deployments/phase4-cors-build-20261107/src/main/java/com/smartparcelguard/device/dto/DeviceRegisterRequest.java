package com.smartparcelguard.device.dto;

import jakarta.validation.constraints.Size;

public record DeviceRegisterRequest(
        @Size(max = 40) String firmwareVersion
) {
}
