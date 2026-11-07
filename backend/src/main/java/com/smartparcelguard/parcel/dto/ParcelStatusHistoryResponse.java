package com.smartparcelguard.parcel.dto;

import com.smartparcelguard.parcel.ParcelStatus;

import java.time.Instant;

public record ParcelStatusHistoryResponse(
        Long id,
        ParcelStatus fromStatus,
        ParcelStatus toStatus,
        String changedBy,
        String note,
        Instant changedAt
) {
}
