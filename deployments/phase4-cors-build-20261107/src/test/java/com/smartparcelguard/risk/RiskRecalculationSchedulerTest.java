package com.smartparcelguard.risk;

import com.smartparcelguard.device.DeviceAssignment;
import com.smartparcelguard.device.DeviceAssignmentRepository;
import com.smartparcelguard.parcel.Parcel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskRecalculationSchedulerTest {

    @Test
    void recalculatesEachAssignedParcelOncePerScan() {
        DeviceAssignmentRepository assignmentRepository = mock(DeviceAssignmentRepository.class);
        RiskAnalysisService riskAnalysisService = mock(RiskAnalysisService.class);
        Parcel parcel = new Parcel();
        parcel.setId(42L);
        DeviceAssignment firstAssignment = assignment(parcel);
        DeviceAssignment duplicateAssignment = assignment(parcel);
        when(assignmentRepository.findActive(null)).thenReturn(List.of(firstAssignment, duplicateAssignment));

        new RiskRecalculationScheduler(assignmentRepository, riskAnalysisService).recalculateAssignedParcels();

        verify(riskAnalysisService).recalculate(42L);
    }

    private static DeviceAssignment assignment(Parcel parcel) {
        DeviceAssignment assignment = new DeviceAssignment();
        assignment.setParcel(parcel);
        return assignment;
    }
}
