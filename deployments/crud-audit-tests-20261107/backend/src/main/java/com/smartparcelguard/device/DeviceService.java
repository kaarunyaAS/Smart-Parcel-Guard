package com.smartparcelguard.device;

import com.smartparcelguard.common.PageResponse;
import com.smartparcelguard.common.TextSanitizer;
import com.smartparcelguard.device.dto.DeviceRegisterRequest;
import com.smartparcelguard.device.dto.DeviceResponse;
import com.smartparcelguard.device.dto.DeviceUpdateRequest;
import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.parcel.ParcelRepository;
import com.smartparcelguard.telemetry.DevicePresence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class DeviceService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "deviceCode", "status", "firmwareVersion");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DeviceRepository deviceRepository;
    private final DeviceAssignmentRepository assignmentRepository;
    private final ParcelRepository parcelRepository;
    private final DevicePresence devicePresence;

    public DeviceService(DeviceRepository deviceRepository,
                         DeviceAssignmentRepository assignmentRepository,
                         ParcelRepository parcelRepository,
                         DevicePresence devicePresence) {
        this.deviceRepository = deviceRepository;
        this.assignmentRepository = assignmentRepository;
        this.parcelRepository = parcelRepository;
        this.devicePresence = devicePresence;
    }

    @Transactional
    public DeviceResponse register(DeviceRegisterRequest request) {
        String rawToken = DeviceTokenHasher.newToken();
        Device device = new Device();
        device.setDeviceCode(nextCode());
        device.setTokenHash(DeviceTokenHasher.sha256(rawToken));
        device.setFirmwareVersion(request == null ? null : TextSanitizer.clean(request.firmwareVersion()));
        device.setStatus(DeviceStatus.OFFLINE);
        device.setEnabled(true);
        deviceRepository.save(device);
        return toResponse(device, rawToken);
    }

    @Transactional
    public PageResponse<DeviceResponse> list(int page, int size, String sort) {
        Page<Device> devices = deviceRepository.findAll(pageRequest(page, size, sort));
        devices.forEach(devicePresence::markOfflineIfStale);
        return PageResponse.from(devices.map(device -> toResponse(device, null)));
    }

    @Transactional
    public DeviceResponse get(Long id) {
        Device device = requireDevice(id);
        devicePresence.markOfflineIfStale(device);
        return toResponse(device, null);
    }

    @Transactional
    public DeviceResponse update(Long id, DeviceUpdateRequest request) {
        if (request.firmwareVersion() == null && request.enabled() == null && request.status() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provide a firmware version, enabled flag, or status");
        }
        Device device = requireDevice(id);
        if (request.firmwareVersion() != null) {
            device.setFirmwareVersion(TextSanitizer.clean(request.firmwareVersion()));
        }
        if (request.enabled() != null) {
            device.setEnabled(request.enabled());
        }
        if (request.status() != null) {
            device.setStatus(request.status());
        }
        return toResponse(device, null);
    }

    @Transactional
    public DeviceResponse assign(Long deviceId, Long parcelId) {
        Device device = requireDevice(deviceId);
        if (!device.isEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "A disabled device cannot be assigned");
        }
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Parcel not found"));
        releaseActiveForDevice(device.getId());
        releaseActiveForParcel(parcel.getId());
        DeviceAssignment assignment = new DeviceAssignment();
        assignment.setDevice(device);
        assignment.setParcel(parcel);
        assignment.setAssignedAt(Instant.now());
        assignment.setActiveDeviceKey(device.getId());
        assignmentRepository.save(assignment);
        return toResponse(device, null);
    }

    @Transactional
    public DeviceResponse unassign(Long deviceId) {
        Device device = requireDevice(deviceId);
        DeviceAssignment assignment = assignmentRepository.findByActiveDeviceKey(device.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Device is not assigned to a parcel"));
        assignment.setReleasedAt(Instant.now());
        assignment.setActiveDeviceKey(null);
        return toResponse(device, null);
    }

    private void releaseActiveForDevice(Long deviceId) {
        assignmentRepository.findByActiveDeviceKey(deviceId).ifPresent(this::release);
    }

    private void releaseActiveForParcel(Long parcelId) {
        List<DeviceAssignment> active = assignmentRepository.findByParcelIdAndActiveDeviceKeyIsNotNull(parcelId);
        active.forEach(this::release);
    }

    private void release(DeviceAssignment assignment) {
        assignment.setReleasedAt(Instant.now());
        assignment.setActiveDeviceKey(null);
    }

    private Device requireDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Device not found"));
    }

    private DeviceResponse toResponse(Device device, String rawToken) {
        DeviceAssignment assignment = assignmentRepository.findByActiveDeviceKey(device.getId()).orElse(null);
        return new DeviceResponse(
                device.getId(),
                device.getDeviceCode(),
                device.getFirmwareVersion(),
                device.getStatus(),
                device.isEnabled(),
                device.getBatteryPercent(),
                device.getGpsStatus(),
                device.getSensorStatus(),
                device.getCameraStatus(),
                device.getLastSeenAt(),
                assignment == null ? null : assignment.getParcel().getId(),
                assignment == null ? null : assignment.getParcel().getParcelCode(),
                devicePresence.isOnline(device),
                rawToken
        );
    }

    private String nextCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = "SPG-D-" + random(10);
            if (deviceRepository.findByDeviceCode(code).isEmpty()) {
                return code;
            }
        }
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Could not generate a unique device code");
    }

    private String random(int length) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return value.toString();
    }

    private PageRequest pageRequest(int page, int size, String sort) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        String[] parts = sort == null ? new String[]{"createdAt", "desc"} : sort.split(",");
        if (!SORT_FIELDS.contains(parts[0])) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(direction, parts[0]));
    }
}
