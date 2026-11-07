package com.smartparcelguard.telemetry;

import com.smartparcelguard.camera.CameraImage;
import com.smartparcelguard.camera.CameraImageRepository;
import com.smartparcelguard.device.Device;
import com.smartparcelguard.device.DeviceAssignment;
import com.smartparcelguard.device.DeviceAssignmentRepository;
import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.parcel.ParcelService;
import com.smartparcelguard.sensor.SensorReadingRepository;
import com.smartparcelguard.telemetry.dto.DevicePresenceResponse;
import com.smartparcelguard.telemetry.dto.EvidenceResponse;
import com.smartparcelguard.telemetry.dto.LocationResponse;
import com.smartparcelguard.telemetry.dto.SensorReadingResponse;
import com.smartparcelguard.tracking.LocationFixRepository;
import com.smartparcelguard.user.User;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.util.List;

@Service
public class TelemetryQueryService {

    private final ParcelService parcelService;
    private final LocationFixRepository locationRepository;
    private final SensorReadingRepository sensorRepository;
    private final CameraImageRepository imageRepository;
    private final DeviceAssignmentRepository assignmentRepository;
    private final DevicePresence devicePresence;
    private final TelemetryService telemetryService;

    public TelemetryQueryService(ParcelService parcelService,
                                 LocationFixRepository locationRepository,
                                 SensorReadingRepository sensorRepository,
                                 CameraImageRepository imageRepository,
                                 DeviceAssignmentRepository assignmentRepository,
                                 DevicePresence devicePresence,
                                 TelemetryService telemetryService) {
        this.parcelService = parcelService;
        this.locationRepository = locationRepository;
        this.sensorRepository = sensorRepository;
        this.imageRepository = imageRepository;
        this.assignmentRepository = assignmentRepository;
        this.devicePresence = devicePresence;
        this.telemetryService = telemetryService;
    }

    @Transactional(readOnly = true)
    public LocationResponse latestLocation(Long parcelId, User actor) {
        parcelService.requireVisibleParcel(parcelId, actor);
        return locationRepository.findFirstByParcelIdOrderByRecordedAtDesc(parcelId)
                .map(telemetryService::toLocation)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No location has been recorded"));
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> locationHistory(Long parcelId, User actor) {
        parcelService.requireVisibleParcel(parcelId, actor);
        return locationRepository.findByParcelIdOrderByRecordedAtAsc(parcelId).stream()
                .map(telemetryService::toLocation)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SensorReadingResponse> sensorHistory(Long parcelId, User actor) {
        parcelService.requireVisibleParcel(parcelId, actor);
        return sensorRepository.findByParcelIdOrderByRecordedAtDesc(parcelId).stream()
                .map(telemetryService::toSensor)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> evidence(Long parcelId, User actor) {
        parcelService.requireVisibleParcel(parcelId, actor);
        return imageRepository.findByParcelIdOrderByCapturedAtDesc(parcelId).stream()
                .map(telemetryService::toEvidence)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvidenceFile evidenceContent(Long parcelId, Long imageId, User actor) {
        parcelService.requireVisibleParcel(parcelId, actor);
        CameraImage image = imageRepository.findByIdAndParcelId(imageId, parcelId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Evidence not found"));
        var path = telemetryService.resolveStoredFile(image.getFilePath());
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Evidence file is missing");
        }
        return new EvidenceFile(new FileSystemResource(path), image.getContentType(), image.getFileName());
    }

    @Transactional
    public DevicePresenceResponse deviceStatus(Long parcelId, User actor) {
        Parcel parcel = parcelService.requireVisibleParcel(parcelId, actor);
        DeviceAssignment assignment = assignmentRepository.findByParcelIdAndActiveDeviceKeyIsNotNull(parcel.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No device is assigned to this parcel"));
        Device device = assignment.getDevice();
        devicePresence.markOfflineIfStale(device);
        return telemetryService.presence(device, parcel.getId());
    }

    public record EvidenceFile(Resource resource, String contentType, String fileName) {
    }
}
