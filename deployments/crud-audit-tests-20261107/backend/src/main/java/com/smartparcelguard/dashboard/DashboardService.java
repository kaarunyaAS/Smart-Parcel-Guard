package com.smartparcelguard.dashboard;

import com.smartparcelguard.alert.AlertService;
import com.smartparcelguard.device.DeviceAssignment;
import com.smartparcelguard.device.DeviceAssignmentRepository;
import com.smartparcelguard.parcel.ParcelRepository;
import com.smartparcelguard.parcel.ParcelStatus;
import com.smartparcelguard.parcel.ParcelStatusHistoryRepository;
import com.smartparcelguard.risk.RiskLevel;
import com.smartparcelguard.telemetry.DevicePresence;
import com.smartparcelguard.user.User;
import com.smartparcelguard.user.UserRole;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final ParcelRepository parcelRepository;
    private final DeviceAssignmentRepository assignmentRepository;
    private final DevicePresence devicePresence;
    private final AlertService alertService;
    private final ParcelStatusHistoryRepository historyRepository;

    public DashboardService(ParcelRepository parcelRepository,
                            DeviceAssignmentRepository assignmentRepository,
                            DevicePresence devicePresence,
                            AlertService alertService,
                            ParcelStatusHistoryRepository historyRepository) {
        this.parcelRepository = parcelRepository;
        this.assignmentRepository = assignmentRepository;
        this.devicePresence = devicePresence;
        this.alertService = alertService;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public DashboardResponse summary(User actor) {
        Long operatorId = actor.getRole() == UserRole.ADMIN ? null : actor.getId();
        int online = 0;
        int offline = 0;
        for (DeviceAssignment assignment : assignmentRepository.findActive(operatorId)) {
            devicePresence.markOfflineIfStale(assignment.getDevice());
            if (devicePresence.isOnline(assignment.getDevice())) {
                online++;
            } else {
                offline++;
            }
        }
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            distribution.put(level.name(), 0L);
        }
        long unscored = 0;
        for (Object[] row : parcelRepository.riskDistribution(operatorId)) {
            if (row[0] == null) {
                unscored += (Long) row[1];
            } else {
                distribution.put(((RiskLevel) row[0]).name(), (Long) row[1]);
            }
        }
        distribution.put("UNSCORED", unscored);
        List<ActivityResponse> activities = historyRepository.recent(operatorId, PageRequest.of(0, 8)).stream()
                .map(history -> new ActivityResponse(
                        history.getParcel().getId(),
                        history.getParcel().getParcelCode(),
                        history.getToStatus().name(),
                        history.getNote(),
                        history.getChangedAt()))
                .toList();
        return new DashboardResponse(
                parcelRepository.countVisible(operatorId, null),
                parcelRepository.countVisible(operatorId, ParcelStatus.IN_TRANSIT),
                parcelRepository.countVisible(operatorId, ParcelStatus.DELIVERED),
                parcelRepository.countVisible(operatorId, ParcelStatus.DELAYED),
                parcelRepository.countByRisk(operatorId, List.of(RiskLevel.HIGH, RiskLevel.CRITICAL)),
                online,
                offline,
                alertService.list(actor, null, false, 8),
                activities,
                distribution
        );
    }

    public record DashboardResponse(
            long totalParcels,
            long inTransit,
            long delivered,
            long delayed,
            long highRisk,
            int devicesOnline,
            int devicesOffline,
            List<AlertService.AlertResponse> recentAlerts,
            List<ActivityResponse> recentActivities,
            Map<String, Long> riskDistribution
    ) {
    }

    public record ActivityResponse(Long parcelId, String parcelCode, String status, String note, Instant changedAt) {
    }
}
