package com.smartparcelguard.risk;

import com.smartparcelguard.alert.AlertType;

import java.time.Instant;

public record RiskContribution(AlertType type, int points, String detail, Instant recordedAt) {
}
