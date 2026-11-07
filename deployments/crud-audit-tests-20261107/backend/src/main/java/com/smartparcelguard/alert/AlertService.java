package com.smartparcelguard.alert;

import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.parcel.ParcelService;
import com.smartparcelguard.risk.RiskLevel;
import com.smartparcelguard.risk.RiskScoreResult;
import com.smartparcelguard.user.User;
import com.smartparcelguard.user.UserRole;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final ParcelService parcelService;

    public AlertService(AlertRepository alertRepository, ParcelService parcelService) {
        this.alertRepository = alertRepository;
        this.parcelService = parcelService;
    }

    @Transactional
    public AlertResponse raiseForRisk(Parcel parcel, RiskScoreResult result, Instant now, int dedupeSeconds) {
        if (result.level() != RiskLevel.HIGH && result.level() != RiskLevel.CRITICAL) {
            return null;
        }
        AlertType alertType = result.correlationApplied() ? AlertType.MULTIPLE_SUSPICIOUS_EVENTS : dominant(result);
        if (alertRepository.existsByParcelIdAndAlertTypeAndStatusIn(
                parcel.getId(), alertType, List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED))) {
            return null;
        }
        String key = result.level().name() + ":" + (now.getEpochSecond() / Math.max(1, dedupeSeconds));
        if (alertRepository.existsByParcelIdAndDedupeKey(parcel.getId(), key)) {
            return null;
        }
        Alert alert = new Alert();
        alert.setParcel(parcel);
        alert.setAlertType(alertType);
        alert.setSeverity(result.level() == RiskLevel.CRITICAL ? AlertSeverity.CRITICAL : AlertSeverity.HIGH);
        alert.setDescription(describe(result));
        alert.setRiskScore(result.score());
        alert.setStatus(AlertStatus.OPEN);
        alert.setTriggeredAt(now);
        alert.setDedupeKey(key);
        return toResponse(alertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> list(User actor, AlertStatus status, boolean unreadOnly, int limit) {
        Long operatorId = actor.getRole() == UserRole.ADMIN ? null : actor.getId();
        return alertRepository.search(operatorId, status, unreadOnly, PageRequest.of(0, Math.min(Math.max(limit, 1), 100)))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AlertResponse markRead(Long id, User actor) {
        Alert alert = requireVisible(id, actor);
        if (alert.getReadAt() == null) {
            alert.setReadAt(Instant.now());
        }
        return toResponse(alert);
    }

    @Transactional
    public AlertResponse acknowledge(Long id, User actor) {
        Alert alert = requireVisible(id, actor);
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new ApiException(HttpStatus.CONFLICT, "A resolved alert cannot be acknowledged");
        }
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(actor);
        alert.setAcknowledgedAt(Instant.now());
        if (alert.getReadAt() == null) {
            alert.setReadAt(alert.getAcknowledgedAt());
        }
        return toResponse(alert);
    }

    @Transactional
    public AlertResponse resolve(Long id, String notes, User actor) {
        Alert alert = requireVisible(id, actor);
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedBy(actor);
        alert.setResolvedAt(Instant.now());
        if (notes != null && !notes.isBlank()) {
            alert.setInvestigationNotes(notes.trim());
        }
        if (alert.getReadAt() == null) {
            alert.setReadAt(alert.getResolvedAt());
        }
        return toResponse(alert);
    }

    private Alert requireVisible(Long id, User actor) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert not found"));
        parcelService.requireVisibleParcel(alert.getParcel().getId(), actor);
        return alert;
    }

    private static AlertType dominant(RiskScoreResult result) {
        return result.contributions().stream()
                .max((left, right) -> Integer.compare(left.points(), right.points()))
                .map(contribution -> contribution.type())
                .orElse(AlertType.MULTIPLE_SUSPICIOUS_EVENTS);
    }

    private static String describe(RiskScoreResult result) {
        if (result.contributions().isEmpty()) {
            return "Parcel risk is " + result.level() + " with score " + result.score();
        }
        String reasons = result.contributions().stream()
                .map(contribution -> contribution.type() + " (" + contribution.points() + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        String correlation = result.correlationApplied() ? " Correlated events added extra points." : "";
        return "Risk " + result.score() + " (" + result.level() + ") from " + reasons + "." + correlation;
    }

    public AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getParcel().getId(),
                alert.getParcel().getParcelCode(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getDescription(),
                alert.getRiskScore(),
                alert.getStatus(),
                alert.getReadAt() == null,
                alert.getTriggeredAt(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt(),
                alert.getInvestigationNotes()
        );
    }

    public record AlertResponse(
            Long id,
            Long parcelId,
            String parcelCode,
            AlertType type,
            AlertSeverity severity,
            String description,
            Integer riskScore,
            AlertStatus status,
            boolean unread,
            Instant triggeredAt,
            Instant acknowledgedAt,
            Instant resolvedAt,
            String notes
    ) {
    }
}
