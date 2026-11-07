package com.smartparcelguard.risk;

import com.smartparcelguard.alert.AlertType;
import com.smartparcelguard.common.DataSource;

import java.time.Instant;

public record RiskFact(AlertType type, Instant recordedAt, String detail, DataSource dataSource) {
}
