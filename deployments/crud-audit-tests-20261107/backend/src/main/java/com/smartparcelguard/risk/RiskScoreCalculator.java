package com.smartparcelguard.risk;

import com.smartparcelguard.alert.AlertType;
import com.smartparcelguard.common.DataSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RiskScoreCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private RiskScoreCalculator() {
    }

    public static List<RiskFact> gpsAnomalies(List<GeoFix> fixes, RiskPolicy policy) {
        List<GeoFix> ordered = fixes.stream()
                .sorted(Comparator.comparing(GeoFix::recordedAt))
                .toList();
        List<RiskFact> anomalies = new ArrayList<>();
        for (int i = 1; i < ordered.size(); i++) {
            GeoFix previous = ordered.get(i - 1);
            GeoFix current = ordered.get(i);
            double meters = distanceMeters(previous.latitude(), previous.longitude(), current.latitude(), current.longitude());
            if (meters < policy.minimumJumpMeters()) {
                continue;
            }
            long seconds = Math.max(1, current.recordedAt().getEpochSecond() - previous.recordedAt().getEpochSecond());
            double speed = meters / seconds;
            if (speed <= policy.maximumPlausibleSpeedMps()) {
                continue;
            }
            anomalies.add(new RiskFact(
                    AlertType.ROUTE_DEVIATION,
                    current.recordedAt(),
                    "Location changed " + Math.round(meters) + " m at about " + Math.round(speed) + " m/s",
                    current.dataSource()
            ));
        }
        return anomalies;
    }

    public static List<RiskFact> repeatedVibration(List<Instant> vibrationTimes,
                                                   Instant now,
                                                   RiskPolicy policy,
                                                   DataSource dataSource) {
        Instant start = now.minusSeconds(policy.correlationSeconds());
        List<Instant> recent = vibrationTimes.stream()
                .filter(time -> !time.isBefore(start))
                .sorted()
                .toList();
        if (recent.size() < policy.vibrationRepeatCount()) {
            return List.of();
        }
        return List.of(new RiskFact(
                AlertType.REPEATED_VIBRATION,
                recent.get(recent.size() - 1),
                recent.size() + " vibration readings inside the correlation window",
                dataSource
        ));
    }

    public static RiskScoreResult score(List<RiskFact> facts, Instant now, RiskPolicy policy) {
        Instant start = now.minusSeconds(policy.correlationSeconds());
        List<RiskFact> recent = facts.stream()
                .filter(fact -> !fact.recordedAt().isBefore(start))
                .sorted(Comparator.comparing(RiskFact::recordedAt))
                .toList();
        List<RiskFact> keptFacts = new ArrayList<>();
        Map<AlertType, Instant> lastKept = new LinkedHashMap<>();
        for (RiskFact fact : recent) {
            Instant previous = lastKept.get(fact.type());
            if (previous != null && fact.recordedAt().getEpochSecond() - previous.getEpochSecond() < policy.dedupeSeconds()) {
                continue;
            }
            keptFacts.add(fact);
            lastKept.put(fact.type(), fact.recordedAt());
        }
        List<RiskContribution> contributions = new ArrayList<>();
        int points = 0;
        for (RiskFact fact : keptFacts) {
            int weight = policy.weightFor(fact.type());
            points += weight;
            contributions.add(new RiskContribution(fact.type(), weight, fact.detail(), fact.recordedAt()));
        }
        boolean correlated = lastKept.size() >= 2;
        if (correlated) {
            points += policy.correlationBonus();
        }
        int score = Math.min(policy.maxScore(), Math.max(0, points));
        return new RiskScoreResult(score, classify(score, policy), correlated, List.copyOf(contributions));
    }

    public static RiskLevel classify(int score, RiskPolicy policy) {
        if (score >= policy.criticalAt()) {
            return RiskLevel.CRITICAL;
        }
        if (score >= policy.highAt()) {
            return RiskLevel.HIGH;
        }
        if (score >= policy.mediumAt()) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double dLat = lat2Rad - lat1Rad;
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }

    public record GeoFix(double latitude, double longitude, Instant recordedAt, DataSource dataSource) {
    }
}
