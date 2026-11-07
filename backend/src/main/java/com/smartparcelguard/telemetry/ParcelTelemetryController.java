package com.smartparcelguard.telemetry;

import com.smartparcelguard.common.ApiResponse;
import com.smartparcelguard.security.CurrentUser;
import com.smartparcelguard.telemetry.dto.DevicePresenceResponse;
import com.smartparcelguard.telemetry.dto.EvidenceResponse;
import com.smartparcelguard.telemetry.dto.LocationResponse;
import com.smartparcelguard.telemetry.dto.SensorReadingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/parcels/{parcelId}")
@Tag(name = "Parcel telemetry")
public class ParcelTelemetryController {

    private final TelemetryQueryService queryService;

    public ParcelTelemetryController(TelemetryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/locations/latest")
    @Operation(summary = "Latest Leaflet-compatible location for a parcel")
    public ApiResponse<LocationResponse> latest(@PathVariable Long parcelId) {
        return ApiResponse.ok("Latest location retrieved", queryService.latestLocation(parcelId, CurrentUser.require()));
    }

    @GetMapping("/locations")
    @Operation(summary = "Location history ordered for a Leaflet polyline")
    public ApiResponse<List<LocationResponse>> history(@PathVariable Long parcelId) {
        return ApiResponse.ok("Location history retrieved", queryService.locationHistory(parcelId, CurrentUser.require()));
    }

    @GetMapping("/sensors")
    @Operation(summary = "Sensor history, including impact and opening detection")
    public ApiResponse<List<SensorReadingResponse>> sensors(@PathVariable Long parcelId) {
        return ApiResponse.ok("Sensor history retrieved", queryService.sensorHistory(parcelId, CurrentUser.require()));
    }

    @GetMapping("/evidence")
    @Operation(summary = "Camera evidence metadata. Image bytes are downloaded separately.")
    public ApiResponse<List<EvidenceResponse>> evidence(@PathVariable Long parcelId) {
        return ApiResponse.ok("Evidence retrieved", queryService.evidence(parcelId, CurrentUser.require()));
    }

    @GetMapping("/evidence/{imageId}/content")
    @Operation(summary = "Download one stored camera image")
    public ResponseEntity<Resource> content(@PathVariable Long parcelId, @PathVariable Long imageId) {
        TelemetryQueryService.EvidenceFile file = queryService.evidenceContent(parcelId, imageId, CurrentUser.require());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .body(file.resource());
    }

    @GetMapping("/device-status")
    @Operation(summary = "Online status from the latest heartbeat and the configured timeout")
    public ApiResponse<DevicePresenceResponse> deviceStatus(@PathVariable Long parcelId) {
        return ApiResponse.ok("Device status retrieved", queryService.deviceStatus(parcelId, CurrentUser.require()));
    }
}
