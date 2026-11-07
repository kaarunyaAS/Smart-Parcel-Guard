package com.smartparcelguard.risk;

import com.smartparcelguard.alert.AlertType;
import com.smartparcelguard.common.DataSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskScoreCalculatorTest {

    private final RiskPolicy policy = new RiskPolicy(25, 50, 10, 20, 15, 15, 100, 300, 600, 5, 25, 50, 75, 100, 55);
    private final Instant now = Instant.parse("2026-11-07T09:00:00Z");

    @Test
    void classifiesScoresIntoFourBands() {
        assertEquals(RiskLevel.LOW, RiskScoreCalculator.classify(0, policy));
        assertEquals(RiskLevel.LOW, RiskScoreCalculator.classify(24, policy));
        assertEquals(RiskLevel.MEDIUM, RiskScoreCalculator.classify(25, policy));
        assertEquals(RiskLevel.HIGH, RiskScoreCalculator.classify(50, policy));
        assertEquals(RiskLevel.CRITICAL, RiskScoreCalculator.classify(75, policy));
    }

    @Test
    void oneVibrationDoesNotAddRiskAndRepeatedVibrationDoes() {
        assertTrue(RiskScoreCalculator.repeatedVibration(List.of(now.minusSeconds(20)), now, policy, DataSource.SIMULATED).isEmpty());
        assertEquals(1, RiskScoreCalculator.repeatedVibration(List.of(
                now.minusSeconds(50), now.minusSeconds(40), now.minusSeconds(30), now.minusSeconds(20), now.minusSeconds(10)
        ), now, policy, DataSource.SIMULATED).size());
    }

    @Test
    void duplicateFactsInsideTheDedupeWindowCountOnce() {
        RiskScoreResult result = RiskScoreCalculator.score(List.of(
                fact(AlertType.STRONG_IMPACT, now.minusSeconds(30)),
                fact(AlertType.STRONG_IMPACT, now.minusSeconds(20))
        ), now, policy);

        assertEquals(25, result.score());
        assertEquals(RiskLevel.MEDIUM, result.level());
        assertFalse(result.correlationApplied());
        assertEquals(1, result.contributions().size());
    }

    @Test
    void differentEventsInsideTheWindowAddCorrelationBonusAndCapAt100() {
        RiskScoreResult result = RiskScoreCalculator.score(List.of(
                fact(AlertType.STRONG_IMPACT, now.minusSeconds(40)),
                fact(AlertType.UNAUTHORIZED_OPENING, now.minusSeconds(30)),
                fact(AlertType.ROUTE_DEVIATION, now.minusSeconds(20)),
                fact(AlertType.DEVICE_OFFLINE, now.minusSeconds(10))
        ), now, policy);

        assertEquals(100, result.score());
        assertEquals(RiskLevel.CRITICAL, result.level());
        assertTrue(result.correlationApplied());
    }

    @Test
    void ordinaryGpsMovementIsIgnoredAndAnImpossibleJumpIsFlagged() {
        assertTrue(RiskScoreCalculator.gpsAnomalies(List.of(
                fix(13.082680, 80.270718, now.minusSeconds(60)),
                fix(13.082800, 80.270900, now)
        ), policy).isEmpty());

        List<RiskFact> anomalies = RiskScoreCalculator.gpsAnomalies(List.of(
                fix(13.082680, 80.270718, now.minusSeconds(30)),
                fix(13.200000, 80.270718, now)
        ), policy);
        assertEquals(1, anomalies.size());
        assertEquals(AlertType.ROUTE_DEVIATION, anomalies.get(0).type());
    }

    private static RiskFact fact(AlertType type, Instant recordedAt) {
        return new RiskFact(type, recordedAt, type.name(), DataSource.SIMULATED);
    }

    private static RiskScoreCalculator.GeoFix fix(double latitude, double longitude, Instant recordedAt) {
        return new RiskScoreCalculator.GeoFix(latitude, longitude, recordedAt, DataSource.SIMULATED);
    }
}
