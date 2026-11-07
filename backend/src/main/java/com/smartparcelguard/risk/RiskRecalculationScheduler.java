package com.smartparcelguard.risk;

import com.smartparcelguard.device.DeviceAssignment;
import com.smartparcelguard.device.DeviceAssignmentRepository;
import com.smartparcelguard.parcel.Parcel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class RiskRecalculationScheduler {

    private final DeviceAssignmentRepository assignmentRepository;
    private final RiskAnalysisService riskAnalysisService;

    public RiskRecalculationScheduler(DeviceAssignmentRepository assignmentRepository,
                                      RiskAnalysisService riskAnalysisService) {
        this.assignmentRepository = assignmentRepository;
        this.riskAnalysisService = riskAnalysisService;
    }

    @Scheduled(
            initialDelayString = "${spg.risk.recalculation-initial-delay-ms:60000}",
            fixedDelayString = "${spg.risk.recalculation-interval-ms:60000}"
    )
    @Transactional
    public void recalculateAssignedParcels() {
        assignmentRepository.findActive(null).stream()
                .map(DeviceAssignment::getParcel)
                .filter(Objects::nonNull)
                .map(Parcel::getId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(riskAnalysisService::recalculate);
    }
}
