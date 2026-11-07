package com.smartparcelguard.telemetry;

import com.smartparcelguard.device.Device;
import com.smartparcelguard.device.DeviceAssignment;
import com.smartparcelguard.device.DeviceAssignmentRepository;
import com.smartparcelguard.device.DeviceRepository;
import com.smartparcelguard.device.DeviceTokenHasher;
import com.smartparcelguard.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceAuthenticationService {

    private final DeviceRepository deviceRepository;
    private final DeviceAssignmentRepository assignmentRepository;

    public DeviceAuthenticationService(DeviceRepository deviceRepository,
                                       DeviceAssignmentRepository assignmentRepository) {
        this.deviceRepository = deviceRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public AuthenticatedDevice authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Device token is required");
        }
        String hash = DeviceTokenHasher.sha256(rawToken.trim());
        Device device = deviceRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid device token"));
        if (!device.isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Device token is revoked");
        }
        DeviceAssignment assignment = assignmentRepository.findByActiveDeviceKey(device.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Device is not assigned to a parcel"));
        return new AuthenticatedDevice(device, assignment.getParcel());
    }
}
