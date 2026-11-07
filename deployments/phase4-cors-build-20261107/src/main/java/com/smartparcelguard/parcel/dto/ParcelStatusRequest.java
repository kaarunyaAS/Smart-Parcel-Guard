package com.smartparcelguard.parcel.dto;

import com.smartparcelguard.parcel.ParcelStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParcelStatusRequest(
        @NotNull ParcelStatus status,
        @Size(max = 500) String note
) {
}
