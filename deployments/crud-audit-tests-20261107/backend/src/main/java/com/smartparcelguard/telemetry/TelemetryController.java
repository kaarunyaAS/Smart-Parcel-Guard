package com.smartparcelguard.telemetry;

import com.smartparcelguard.camera.CameraRole;
import com.smartparcelguard.common.ApiResponse;
import com.smartparcelguard.common.DataSource;
import com.smartparcelguard.exception.ApiException;
import com.smartparcelguard.telemetry.dto.DevicePresenceResponse;
import com.smartparcelguard.telemetry.dto.EvidenceResponse;
import com.smartparcelguard.telemetry.dto.GpsIngestRequest;
import com.smartparcelguard.telemetry.dto.HeartbeatRequest;
import com.smartparcelguard.telemetry.dto.LocationResponse;
import com.smartparcelguard.telemetry.dto.SensorIngestRequest;
import com.smartparcelguard.telemetry.dto.SensorReadingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

@RestController
@RequestMapping("/api/telemetry")
@Tag(name = "Telemetry")
@SecurityRequirements(value = @SecurityRequirement(name = "deviceToken"))
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/gps")
    @Operation(summary = "Store a GPS fix from the assigned device. Declare dataSource as REAL or SIMULATED.")
    public ResponseEntity<ApiResponse<LocationResponse>> gps(@Valid @RequestBody GpsIngestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Location stored", telemetryService.ingestGps(request)));
    }

    @PostMapping("/sensors")
    @Operation(summary = "Store an MPU6050 and reed-switch reading. Impact requires both magnitude and duration.")
    public ResponseEntity<ApiResponse<SensorReadingResponse>> sensors(@Valid @RequestBody SensorIngestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sensor reading stored", telemetryService.ingestSensors(request)));
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Record a device heartbeat and mark it online")
    public ApiResponse<DevicePresenceResponse> heartbeat(@Valid @RequestBody(required = false) HeartbeatRequest request) {
        return ApiResponse.ok("Heartbeat accepted", telemetryService.heartbeat(request));
    }

    @PostMapping(value = "/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload one inside or outside camera image. This is not a live video stream.")
    public ResponseEntity<ApiResponse<EvidenceResponse>> evidence(
            @RequestPart("file") MultipartFile file,
            @RequestParam CameraRole cameraRole,
            @RequestParam DataSource dataSource,
            @RequestParam(required = false) Instant capturedAt) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image file is empty");
        }
        try {
            EvidenceResponse stored = telemetryService.storeEvidence(
                    file.getBytes(),
                    file.getContentType(),
                    file.getOriginalFilename(),
                    cameraRole,
                    dataSource,
                    capturedAt);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Evidence stored", stored));
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image file could not be read");
        }
    }
}
