package com.smartparcelguard.risk;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RiskSettings(
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal impactThresholdG,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal vibrationThresholdG,
        @NotNull @Min(1) @Max(60_000) Integer impactMinDurationMs,
        @NotNull @Min(1) @Max(86_400) Integer deviceOfflineAfterSeconds,
        @NotNull @Min(1) @Max(86_400) Integer riskDedupeWindowSeconds,
        @NotNull @Min(0) @Max(100) Integer riskStrongImpactPoints,
        @NotNull @Min(0) @Max(100) Integer riskUnauthorizedOpeningPoints,
        @NotNull @Min(0) @Max(100) Integer riskUnusualVibrationPoints,
        @NotNull @Min(0) @Max(100) Integer riskRouteDeviationPoints,
        @NotNull @Min(0) @Max(100) Integer riskDeviceOfflinePoints,
        @NotNull @Min(0) @Max(100) Integer riskCorrelationBonusPoints,
        @NotNull @Min(1) @Max(100) Integer riskMaxScore,
        @NotNull @Min(1) @Max(86_400) Integer riskCorrelationWindowSeconds,
        @NotNull @Min(1) @Max(100) Integer riskVibrationRepeatCount,
        @NotNull @Min(1) @Max(100) Integer riskLevelMedium,
        @NotNull @Min(2) @Max(100) Integer riskLevelHigh,
        @NotNull @Min(3) @Max(100) Integer riskLevelCritical,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1000000.0") BigDecimal gpsAnomalyMinimumJumpMeters,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1000.0") BigDecimal gpsAnomalyMaximumPlausibleSpeedMps
) {
}
