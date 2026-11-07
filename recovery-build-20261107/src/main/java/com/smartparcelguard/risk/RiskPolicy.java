package com.smartparcelguard.risk;

import com.smartparcelguard.alert.AlertType;

public record RiskPolicy(
        int impactPoints,
        int openingPoints,
        int vibrationPoints,
        int gpsPoints,
        int offlinePoints,
        int correlationBonus,
        int maxScore,
        int dedupeSeconds,
        int correlationSeconds,
        int vibrationRepeatCount,
        int mediumAt,
        int highAt,
        int criticalAt,
        double minimumJumpMeters,
        double maximumPlausibleSpeedMps
) {
    public int weightFor(AlertType type) {
        return switch (type) {
            case STRONG_IMPACT -> impactPoints;
            case UNAUTHORIZED_OPENING -> openingPoints;
            case REPEATED_VIBRATION -> vibrationPoints;
            case ROUTE_DEVIATION, UNEXPECTED_MOVEMENT -> gpsPoints;
            case DEVICE_OFFLINE -> offlinePoints;
            case MULTIPLE_SUSPICIOUS_EVENTS, LOW_BATTERY -> 0;
        };
    }
}
