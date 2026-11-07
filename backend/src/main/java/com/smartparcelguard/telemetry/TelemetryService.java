package com.smartparcelguard.telemetry;

import com.smartparcelguard.camera.CameraImage;
import com.smartparcelguard.camera.CameraImageRepository;
import com.smartparcelguard.camera.CameraRole;
import com.smartparcelguard.common.DataSource;
import com.smartparcelguard.config.SmartParcelGuardProperties;
import com.smartparcelguard.device.Device;
import com.smartparcelguard.device.DeviceRepository;
import com.smartparcelguard.device.DeviceStatus;
import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.parcel.ParcelRepository;
import com.smartparcelguard.sensor.SensorReading;
import com.smartparcelguard.sensor.SensorReadingRepository;
import com.smartparcelguard.tamper.TamperEvent;
import com.smartparcelguard.tamper.TamperEventRepository;
import com.smartparcelguard.tamper.TamperEventType;
import com.smartparcelguard.alert.AlertSeverity;
import com.smartparcelguard.telemetry.dto.DevicePresenceResponse;
import com.smartparcelguard.telemetry.dto.EvidenceResponse;
import com.smartparcelguard.telemetry.dto.GpsIngestRequest;
import com.smartparcelguard.telemetry.dto.HeartbeatRequest;
import com.smartparcelguard.telemetry.dto.LocationResponse;
import com.smartparcelguard.telemetry.dto.SensorIngestRequest;
import com.smartparcelguard.telemetry.dto.SensorReadingResponse;
import com.smartparcelguard.tracking.GpsSignalStatus;
import com.smartparcelguard.tracking.LocationFix;
import com.smartparcelguard.tracking.LocationFixRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TelemetryService {

    private final LocationFixRepository locationRepository;
    private final SensorReadingRepository sensorRepository;
    private final TamperEventRepository tamperRepository;
    private final CameraImageRepository imageRepository;
    private final DeviceRepository deviceRepository;
    private final ParcelRepository parcelRepository;
    private final ThresholdSettings thresholds;
    private final SmartParcelGuardProperties properties;
    private final com.smartparcelguard.risk.RiskAnalysisService riskAnalysisService;

    public TelemetryService(LocationFixRepository locationRepository,
                            SensorReadingRepository sensorRepository,
                            TamperEventRepository tamperRepository,
                            CameraImageRepository imageRepository,
                            DeviceRepository deviceRepository,
                            ParcelRepository parcelRepository,
                            ThresholdSettings thresholds,
                            SmartParcelGuardProperties properties,
                            com.smartparcelguard.risk.RiskAnalysisService riskAnalysisService) {
        this.locationRepository = locationRepository;
        this.sensorRepository = sensorRepository;
        this.tamperRepository = tamperRepository;
        this.imageRepository = imageRepository;
        this.deviceRepository = deviceRepository;
        this.parcelRepository = parcelRepository;
        this.thresholds = thresholds;
        this.properties = properties;
        this.riskAnalysisService = riskAnalysisService;
    }

    @Transactional
    public LocationResponse ingestGps(GpsIngestRequest request) {
        AuthenticatedDevice session = DeviceSession.require();
        GpsCoordinates.validate(request.latitude(), request.longitude(), request.altitudeMeters(),
                request.speedMps(), request.recordedAt(), Instant.now());
        Device device = reloadDevice(session);
        Parcel parcel = reloadParcel(session);
        LocationFix fix = new LocationFix();
        fix.setParcel(parcel);
        fix.setDevice(device);
        fix.setLatitude(request.latitude());
        fix.setLongitude(request.longitude());
        fix.setAltitudeMeters(request.altitudeMeters());
        fix.setSpeedMps(request.speedMps());
        fix.setAccuracyMeters(request.accuracyMeters());
        fix.setRecordedAt(request.recordedAt());
        fix.setDataSource(request.dataSource());
        fix.setGpsSignalStatus(request.gpsSignalStatus() == null ? GpsSignalStatus.UNKNOWN : request.gpsSignalStatus());
        LocationFix saved = locationRepository.save(fix);
        LocationResponse response = toLocation(saved);
        riskAnalysisService.recalculate(parcel, "LOCATION", response);
        return response;
    }

    @Transactional
    public SensorReadingResponse ingestSensors(SensorIngestRequest request) {
        AuthenticatedDevice session = DeviceSession.require();
        if (request.recordedAt().isAfter(Instant.now().plus(GpsCoordinates.FUTURE_TOLERANCE))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "recordedAt cannot be more than five minutes in the future");
        }
        Device device = reloadDevice(session);
        Parcel parcel = reloadParcel(session);
        SensorAssessment assessment = SensorEventDetector.assess(
                request.accelerationX(),
                request.accelerationY(),
                request.accelerationZ(),
                request.durationMs(),
                request.reedSwitch(),
                parcel.getStatus(),
                thresholds.vibrationThresholdG(),
                thresholds.impactThresholdG(),
                thresholds.minimumImpactDurationMs());
        SensorReading reading = new SensorReading();
        reading.setParcel(parcel);
        reading.setDevice(device);
        reading.setAccelerationX(request.accelerationX());
        reading.setAccelerationY(request.accelerationY());
        reading.setAccelerationZ(request.accelerationZ());
        reading.setGyroscopeX(request.gyroscopeX());
        reading.setGyroscopeY(request.gyroscopeY());
        reading.setGyroscopeZ(request.gyroscopeZ());
        reading.setMagnitude(assessment.magnitude());
        reading.setReedSwitch(request.reedSwitch());
        reading.setMovementStatus(assessment.movementStatus());
        reading.setImpactDetected(assessment.impactDetected());
        reading.setUnauthorizedOpening(assessment.unauthorizedOpening());
        reading.setRecordedAt(request.recordedAt());
        reading.setDataSource(request.dataSource());
        sensorRepository.save(reading);
        if (assessment.unauthorizedOpening()) {
            recordOpening(parcel, device, request.recordedAt());
        }
        SensorReadingResponse response = toSensor(reading);
        riskAnalysisService.recalculate(parcel, "SENSOR", response);
        return response;
    }

    @Transactional
    public DevicePresenceResponse heartbeat(HeartbeatRequest request) {
        Device device = reloadDevice(DeviceSession.require());
        Parcel parcel = reloadParcel(DeviceSession.require());
        device.setLastSeenAt(Instant.now());
        device.setStatus(DeviceStatus.ONLINE);
        if (request != null) {
            if (request.batteryPercent() != null) {
                device.setBatteryPercent(request.batteryPercent());
            }
            if (request.gpsStatus() != null) {
                device.setGpsStatus(request.gpsStatus());
            }
            if (request.sensorStatus() != null) {
                device.setSensorStatus(request.sensorStatus());
            }
            if (request.cameraStatus() != null) {
                device.setCameraStatus(request.cameraStatus());
            }
        }
        DevicePresenceResponse response = presence(device, parcel.getId());
        riskAnalysisService.recalculate(parcel, "DEVICE", response);
        return response;
    }

    @Transactional
    public EvidenceResponse storeEvidence(byte[] bytes,
                                          String declaredContentType,
                                          String originalName,
                                          CameraRole cameraRole,
                                          DataSource dataSource,
                                          Instant capturedAt) {
        if (cameraRole == null || dataSource == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "cameraRole and dataSource are required");
        }
        Instant recordedAt = capturedAt == null ? Instant.now() : capturedAt;
        if (recordedAt.isAfter(Instant.now().plus(GpsCoordinates.FUTURE_TOLERANCE))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "capturedAt cannot be more than five minutes in the future");
        }
        ImageEvidenceValidator.DetectedImage detected = ImageEvidenceValidator.validate(bytes, declaredContentType);
        AuthenticatedDevice session = DeviceSession.require();
        Device device = reloadDevice(session);
        Parcel parcel = reloadParcel(session);
        String fileName = UUID.randomUUID() + "." + detected.extension();
        String relative = parcel.getId() + "/" + fileName;
        Path target = resolveStoredFile(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Evidence could not be stored");
        }
        CameraImage image = new CameraImage();
        image.setParcel(parcel);
        image.setDevice(device);
        image.setCameraRole(cameraRole);
        image.setFilePath(relative.replace('\\', '/'));
        image.setFileName(safeOriginalName(originalName, fileName));
        image.setContentType(detected.contentType());
        image.setByteSize((long) bytes.length);
        image.setChecksumSha256(sha256(bytes));
        image.setCapturedAt(recordedAt);
        image.setDataSource(dataSource);
        CameraImage saved = imageRepository.save(image);
        EvidenceResponse response = toEvidence(saved);
        riskAnalysisService.recalculate(parcel, "EVIDENCE", response);
        return response;
    }

    public DevicePresenceResponse presence(Device device, Long parcelId) {
        boolean online = DevicePresenceRules.online(
                device.getLastSeenAt(), device.isEnabled(), Instant.now(), thresholds.offlineAfterSeconds());
        return new DevicePresenceResponse(
                device.getId(),
                device.getDeviceCode(),
                parcelId,
                DevicePresenceRules.statusAfterTimeout(device.getStatus(), online),
                online,
                device.getLastSeenAt(),
                thresholds.offlineAfterSeconds(),
                device.getBatteryPercent(),
                device.getGpsStatus(),
                device.getSensorStatus(),
                device.getCameraStatus()
        );
    }

    public LocationResponse toLocation(LocationFix fix) {
        return new LocationResponse(
                fix.getId(),
                fix.getParcel().getId(),
                fix.getDevice().getId(),
                fix.getLatitude(),
                fix.getLongitude(),
                fix.getAltitudeMeters(),
                fix.getSpeedMps(),
                fix.getAccuracyMeters(),
                fix.getRecordedAt(),
                fix.getDataSource(),
                fix.getGpsSignalStatus(),
                new BigDecimal[]{fix.getLatitude(), fix.getLongitude()}
        );
    }

    public SensorReadingResponse toSensor(SensorReading reading) {
        return new SensorReadingResponse(
                reading.getId(),
                reading.getParcel().getId(),
                reading.getDevice().getId(),
                reading.getAccelerationX(),
                reading.getAccelerationY(),
                reading.getAccelerationZ(),
                reading.getGyroscopeX(),
                reading.getGyroscopeY(),
                reading.getGyroscopeZ(),
                reading.getMagnitude(),
                reading.getReedSwitch(),
                reading.getMovementStatus(),
                reading.isImpactDetected(),
                reading.isUnauthorizedOpening(),
                reading.getRecordedAt(),
                reading.getDataSource()
        );
    }

    public EvidenceResponse toEvidence(CameraImage image) {
        return new EvidenceResponse(
                image.getId(),
                image.getParcel().getId(),
                image.getDevice().getId(),
                image.getCameraRole(),
                image.getFileName(),
                image.getContentType(),
                image.getByteSize(),
                image.getChecksumSha256(),
                image.getCapturedAt(),
                image.getDataSource(),
                "/api/parcels/" + image.getParcel().getId() + "/evidence/" + image.getId() + "/content"
        );
    }

    public Path resolveStoredFile(String relativePath) {
        Path root = Path.of(properties.getStorage().getImageDir()).toAbsolutePath().normalize();
        Path file = root.resolve(relativePath).normalize();
        if (!file.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Evidence path is invalid");
        }
        return file;
    }

    private void recordOpening(Parcel parcel, Device device, Instant recordedAt) {
        Instant windowStart = recordedAt.minusSeconds(thresholds.dedupeWindowSeconds());
        boolean recent = tamperRepository.existsByParcelIdAndEventTypeAndRecordedAtAfter(
                parcel.getId(), TamperEventType.OPENED, windowStart);
        if (recent) {
            return;
        }
        TamperEvent event = new TamperEvent();
        event.setParcel(parcel);
        event.setDevice(device);
        event.setEventType(TamperEventType.OPENED);
        event.setRecordedAt(recordedAt);
        event.setSeverity(AlertSeverity.HIGH);
        event.setDuringMaintenance(false);
        tamperRepository.save(event);
    }

    private Device reloadDevice(AuthenticatedDevice session) {
        return deviceRepository.findById(session.device().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid device token"));
    }

    private Parcel reloadParcel(AuthenticatedDevice session) {
        return parcelRepository.findById(session.parcel().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Device is not assigned to a parcel"));
    }

    private static String safeOriginalName(String originalName, String storedName) {
        if (originalName == null || originalName.isBlank()) {
            return storedName;
        }
        String cleaned = Path.of(originalName).getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        if (cleaned.isBlank() || cleaned.length() > 255) {
            return storedName;
        }
        return cleaned;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
