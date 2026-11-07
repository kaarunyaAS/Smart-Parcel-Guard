package com.smartparcelguard.parcel.dto;

import com.smartparcelguard.parcel.MonitoringState;
import com.smartparcelguard.parcel.ParcelPriority;
import com.smartparcelguard.parcel.ParcelStatus;
import com.smartparcelguard.risk.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ParcelResponse(
        Long id,
        String parcelCode,
        String name,
        String description,
        String category,
        BigDecimal declaredValue,
        String senderName,
        String senderPhone,
        String senderAddress,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        String pickupAddress,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        String destinationAddress,
        BigDecimal destinationLatitude,
        BigDecimal destinationLongitude,
        LocalDate expectedDeliveryDate,
        ParcelPriority priority,
        ParcelStatus status,
        MonitoringState monitoringState,
        Long operatorId,
        String operatorUsername,
        Long deviceId,
        String deviceCode,
        Integer latestRiskScore,
        RiskLevel latestRiskLevel,
        Instant createdAt
) {
}
