package com.smartparcelguard.device.dto;

import jakarta.validation.constraints.NotNull;

public record AssignDeviceRequest(
        @NotNull Long parcelId
) {
}
