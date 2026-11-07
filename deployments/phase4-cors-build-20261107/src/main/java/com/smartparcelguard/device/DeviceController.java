package com.smartparcelguard.device;

import com.smartparcelguard.common.ApiResponse;
import com.smartparcelguard.common.PageResponse;
import com.smartparcelguard.device.dto.AssignDeviceRequest;
import com.smartparcelguard.device.dto.DeviceRegisterRequest;
import com.smartparcelguard.device.dto.DeviceResponse;
import com.smartparcelguard.device.dto.DeviceUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @Operation(summary = "Register a device. The device token is returned only in this response.")
    public ResponseEntity<ApiResponse<DeviceResponse>> register(@Valid @RequestBody(required = false) DeviceRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Device registered", deviceService.register(request)));
    }

    @GetMapping
    @Operation(summary = "List devices")
    public ApiResponse<PageResponse<DeviceResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.ok("Devices retrieved", deviceService.list(page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "View a device")
    public ApiResponse<DeviceResponse> get(@PathVariable Long id) {
        return ApiResponse.ok("Device retrieved", deviceService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update firmware, activation, or device status")
    public ApiResponse<DeviceResponse> update(@PathVariable Long id, @Valid @RequestBody DeviceUpdateRequest request) {
        return ApiResponse.ok("Device updated", deviceService.update(id, request));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign a device to a parcel")
    public ApiResponse<DeviceResponse> assign(@PathVariable Long id, @Valid @RequestBody AssignDeviceRequest request) {
        return ApiResponse.ok("Device assigned", deviceService.assign(id, request.parcelId()));
    }

    @PostMapping("/{id}/unassign")
    @Operation(summary = "Remove the active parcel assignment from a device")
    public ApiResponse<DeviceResponse> unassign(@PathVariable Long id) {
        return ApiResponse.ok("Device unassigned", deviceService.unassign(id));
    }
}
