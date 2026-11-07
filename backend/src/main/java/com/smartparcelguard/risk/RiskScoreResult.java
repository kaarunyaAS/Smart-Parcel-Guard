package com.smartparcelguard.risk;

import java.util.List;

public record RiskScoreResult(
        int score,
        RiskLevel level,
        boolean correlationApplied,
        List<RiskContribution> contributions
) {
}
