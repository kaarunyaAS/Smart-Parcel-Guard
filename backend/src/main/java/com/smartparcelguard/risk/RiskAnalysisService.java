package com.smartparcelguard.risk;

import com.smartparcelguard.alert.AlertSeverity;
import com.smartparcelguard.alert.AlertType;
import com.smartparcelguard.alert.SecurityEvent;
import com.smartparcelguard.alert.SecurityEventRepository;
import com.smartparcelguard.common.DataSource;
import com.smartparcelguard.device.Device;
import com.smartparcelguard.device.DeviceAssignmentRepository;
import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.parcel.ParcelRepository;
import com.smartparcelguard.parcel.ParcelService;
import com.smartparcelguard.parcel.ParcelStatus;
import com.smartparcelguard.realtime.LiveUpdateBroker;
import com.smartparcelguard.sensor.MovementStatus;
import com.smartparcelguard.sensor.SensorReading;
import com.smartparcelguard.sensor.SensorReadingRepository;
import com.smartparcelguard.tracking.LocationFix;
import com.smartparcelguard.tracking.LocationFixRepository;
import com.smartparcelguard.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class RiskAnalysisService {

    private static final EnumSet<ParcelStatus> IN_CUSTODY = EnumSet.of(
            ParcelStatus.READY_FOR_PICKUP,
            ParcelStatus.PICKED_UP,
            ParcelStatus.IN_TRANSIT,
            ParcelStatus.DELAYED,
            ParcelStatus.UNDER_INVESTIGATION,
            ParcelStatus.OUT_FOR_DELIVERY
    );

    private final RiskPolicyFactory policies;
    private final SensorReadingRepository sensorRepository;
    private final LocationFixRepository locationRepository;
    private final SecurityEventRepository securityEventRepository;
    private final RiskAnalysisRepository analysisRepository;
    private final ParcelRepository parcelRepository;
    private final DeviceAssignmentRepository assignmentRepository;
    private final ParcelService parcelService;
    private final com.smartparcelguard.alert.AlertService alertService;
    private final LiveUpdateBroker liveUpdates;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RiskAnalysisService(RiskPolicyFactory policies,
                               SensorReadingRepository sensorRepository,
                               LocationFixRepository locationRepository,
                               SecurityEventRepository securityEventRepository,
                               RiskAnalysisRepository analysisRepository,
                               ParcelRepository parcelRepository,
                               DeviceAssignmentRepository assignmentRepository,
                               ParcelService parcelService,
                               com.smartparcelguard.alert.AlertService alertService,
                               LiveUpdateBroker liveUpdates,
                               ObjectMapper objectMapper,
                               Clock clock) {
        this.policies = policies;
        this.sensorRepository = sensorRepository;
        this.locationRepository = locationRepository;
        this.securityEventRepository = securityEventRepository;
        this.analysisRepository = analysisRepository;
        this.parcelRepository = parcelRepository;
        this.assignmentRepository = assignmentRepository;
        this.parcelService = parcelService;
        this.alertService = alertService;
        this.liveUpdates = liveUpdates;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public RiskView recalculateFor(Long parcelId, User actor) {
        parcelService.requireVisibleParcel(parcelId, actor);
        return recalculate(parcelId);
    }

    @Transactional
    public RiskView recalculate(Long parcelId) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Parcel not found"));
        return recalculate(parcel, null, null);
    }

    @Transactional
    public RiskView recalculate(Parcel parcel, String updateType, Object update) {
        RiskPolicy policy = policies.current();
        Instant now = Instant.now(clock);
        Instant windowStart = now.minusSeconds(Math.max(policy.correlationSeconds(), policy.dedupeSeconds()));
        List<RiskFact> facts = factsFor(parcel, policy, now, windowStart);
        rememberEvents(parcel, facts, policy);
        RiskScoreResult result = RiskScoreCalculator.score(facts, now, policy);
        String reasons = reasonsJson(result);
        RiskAnalysis latest = analysisRepository.findFirstByParcelIdOrderByCalculatedAtDesc(parcel.getId()).orElse(null);
        boolean changed = latest == null
                || latest.getScore() != result.score()
                || latest.getRiskLevel() != result.level()
                || !reasons.equals(latest.getReasons());
        if (changed) {
            RiskAnalysis analysis = new RiskAnalysis();
            analysis.setParcel(parcel);
            analysis.setScore(result.score());
            analysis.setRiskLevel(result.level());
            analysis.setReasons(reasons);
            analysis.setCorrelationApplied(result.correlationApplied());
            analysis.setCalculatedAt(now);
            analysisRepository.save(analysis);
        }
        parcel.setLatestRiskScore(result.score());
        parcel.setLatestRiskLevel(result.level());
        RiskView view = toView(parcel.getId(), result, now, reasons);
        var alert = alertService.raiseForRisk(parcel, result, now, policy.dedupeSeconds());
        if (updateType != null) {
            liveUpdates.publish(parcel, updateType, update);
        }
        if (changed) {
            liveUpdates.publish(parcel, "RISK", view);
        }
        if (alert != null) {
            liveUpdates.publish(parcel, "ALERT", alert);
        }
        return view;
    }

    @Transactional(readOnly = true)
    public RiskView latest(Long parcelId, User actor) {
        parcelService.requireVisibleParcel(parcelId, actor);
        RiskAnalysis analysis = analysisRepository.findFirstByParcelIdOrderByCalculatedAtDesc(parcelId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No risk score has been calculated"));
        return new RiskView(parcelId, analysis.getScore(), analysis.getRiskLevel(), analysis.isCorrelationApplied(),
                analysis.getReasons(), analysis.getCalculatedAt());
    }

    @Transactional(readOnly = true)
    public List<RiskView> history(Long parcelId, User actor) {
        parcelService.requireVisibleParcel(parcelId, actor);
        return analysisRepository.findByParcelIdOrderByCalculatedAtAsc(parcelId).stream()
                .map(analysis -> new RiskView(parcelId, analysis.getScore(), analysis.getRiskLevel(),
                        analysis.isCorrelationApplied(), analysis.getReasons(), analysis.getCalculatedAt()))
                .toList();
    }

    private List<RiskFact> factsFor(Parcel parcel, RiskPolicy policy, Instant now, Instant windowStart) {
        List<RiskFact> facts = new ArrayList<>();
        List<SensorReading> readings = sensorRepository.findByParcelIdAndRecordedAtAfterOrderByRecordedAtAsc(parcel.getId(), windowStart);
        List<Instant> vibrations = new ArrayList<>();
        DataSource vibrationSource = DataSource.SIMULATED;
        for (SensorReading reading : readings) {
            if (reading.isImpactDetected()) {
                facts.add(new RiskFact(AlertType.STRONG_IMPACT, reading.getRecordedAt(),
                        "Sustained acceleration above the impact threshold", reading.getDataSource()));
            } else if (reading.getMovementStatus() == MovementStatus.VIBRATION) {
                vibrations.add(reading.getRecordedAt());
                if (reading.getDataSource() == DataSource.REAL) {
                    vibrationSource = DataSource.REAL;
                }
            }
            if (reading.isUnauthorizedOpening()) {
                facts.add(new RiskFact(AlertType.UNAUTHORIZED_OPENING, reading.getRecordedAt(),
                        "Reed switch opened while the parcel was in custody", reading.getDataSource()));
            }
        }
        facts.addAll(RiskScoreCalculator.repeatedVibration(vibrations, now, policy, vibrationSource));
        List<LocationFix> fixes = locationRepository.findByParcelIdAndRecordedAtAfterOrderByRecordedAtAsc(parcel.getId(), windowStart);
        facts.addAll(RiskScoreCalculator.gpsAnomalies(fixes.stream()
                .map(fix -> new RiskScoreCalculator.GeoFix(
                        fix.getLatitude().doubleValue(),
                        fix.getLongitude().doubleValue(),
                        fix.getRecordedAt(),
                        fix.getDataSource()))
                .toList(), policy));
        if (IN_CUSTODY.contains(parcel.getStatus())) {
            assignmentRepository.findByParcelIdAndActiveDeviceKeyIsNotNull(parcel.getId()).stream()
                    .findFirst()
                    .map(assignment -> assignment.getDevice())
                    .filter(device -> device.getLastSeenAt() != null)
                    .filter(device -> device.getLastSeenAt().isBefore(now.minusSeconds(policies.offlineSeconds())))
                    .ifPresent(device -> facts.add(new RiskFact(
                            AlertType.DEVICE_OFFLINE,
                            device.getLastSeenAt(),
                            "Assigned device has not sent a heartbeat within the timeout",
                            DataSource.REAL
                    )));
        }
        return facts;
    }

    private void rememberEvents(Parcel parcel, List<RiskFact> facts, RiskPolicy policy) {
        Device device = assignmentRepository.findByParcelIdAndActiveDeviceKeyIsNotNull(parcel.getId()).stream()
                .findFirst()
                .map(assignment -> assignment.getDevice())
                .orElse(null);
        for (RiskFact fact : facts) {
            String key = fact.type().name() + ":" + (fact.recordedAt().getEpochSecond() / Math.max(1, policy.dedupeSeconds()));
            if (securityEventRepository.findByParcelIdAndDedupeKey(parcel.getId(), key).isPresent()) {
                continue;
            }
            SecurityEvent event = new SecurityEvent();
            event.setParcel(parcel);
            event.setDevice(device);
            event.setEventType(fact.type());
            event.setSeverity(severity(fact.type()));
            event.setDescription(fact.detail());
            event.setRecordedAt(fact.recordedAt());
            event.setRiskPoints(policy.weightFor(fact.type()));
            event.setDedupeKey(key);
            event.setDataSource(fact.dataSource() == null ? DataSource.SIMULATED : fact.dataSource());
            securityEventRepository.save(event);
        }
    }

    private static AlertSeverity severity(AlertType type) {
        return switch (type) {
            case UNAUTHORIZED_OPENING, STRONG_IMPACT -> AlertSeverity.HIGH;
            case ROUTE_DEVIATION, DEVICE_OFFLINE, REPEATED_VIBRATION -> AlertSeverity.MEDIUM;
            default -> AlertSeverity.LOW;
        };
    }

    private String reasonsJson(RiskScoreResult result) {
        try {
            return objectMapper.writeValueAsString(new ReasonDocument(
                    result.contributions().isEmpty() ? "No suspicious events in the correlation window" : "Risk calculated from recent telemetry",
                    result.correlationApplied(),
                    result.contributions()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not store risk reasons", exception);
        }
    }

    private static RiskView toView(Long parcelId, RiskScoreResult result, Instant calculatedAt, String reasons) {
        return new RiskView(parcelId, result.score(), result.level(), result.correlationApplied(), reasons, calculatedAt);
    }

    public record RiskView(Long parcelId, int score, RiskLevel level, boolean correlationApplied, String reasons, Instant calculatedAt) {
    }

    private record ReasonDocument(String summary, boolean correlationApplied, List<RiskContribution> contributions) {
    }
}
