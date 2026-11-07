package com.smartparcelguard.parcel.dto;

import com.smartparcelguard.parcel.ParcelPriority;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 80) String category,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal declaredValue,
        @NotBlank @Size(max = 160) String senderName,
        @Pattern(regexp = "^[0-9+()\\-\\s]{6,30}$") String senderPhone,
        @Size(max = 500) String senderAddress,
        @NotBlank @Size(max = 160) String receiverName,
        @Pattern(regexp = "^[0-9+()\\-\\s]{6,30}$") String receiverPhone,
        @Size(max = 500) String receiverAddress,
        @NotBlank @Size(max = 500) String pickupAddress,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal pickupLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal pickupLongitude,
        @NotBlank @Size(max = 500) String destinationAddress,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal destinationLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal destinationLongitude,
        LocalDate expectedDeliveryDate,
        @NotNull ParcelPriority priority,
        Long operatorId,
        Long deviceId
) {
}
