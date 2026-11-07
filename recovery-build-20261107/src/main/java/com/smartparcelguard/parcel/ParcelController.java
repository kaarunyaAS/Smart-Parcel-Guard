package com.smartparcelguard.parcel;

import com.smartparcelguard.common.ApiResponse;
import com.smartparcelguard.common.PageResponse;
import com.smartparcelguard.parcel.dto.ParcelRequest;
import com.smartparcelguard.parcel.dto.ParcelResponse;
import com.smartparcelguard.parcel.dto.ParcelStatusHistoryResponse;
import com.smartparcelguard.parcel.dto.ParcelStatusRequest;
import com.smartparcelguard.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/parcels")
@Tag(name = "Parcels")
public class ParcelController {

    private final ParcelService parcelService;

    public ParcelController(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @PostMapping
    @Operation(summary = "Register a parcel and generate its parcel code")
    public ResponseEntity<ApiResponse<ParcelResponse>> create(@Valid @RequestBody ParcelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Parcel registered", parcelService.create(request, CurrentUser.require())));
    }

    @GetMapping
    @Operation(summary = "Search and filter parcels. Operators see only parcels assigned to them.")
    public ApiResponse<PageResponse<ParcelResponse>> list(
            @RequestParam(required = false) ParcelStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.ok("Parcels retrieved",
                parcelService.list(CurrentUser.require(), status, query, page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "View a parcel")
    public ApiResponse<ParcelResponse> get(@PathVariable Long id) {
        return ApiResponse.ok("Parcel retrieved", parcelService.get(id, CurrentUser.require()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update parcel details. A device id, when present, replaces the active assignment.")
    public ApiResponse<ParcelResponse> update(@PathVariable Long id, @Valid @RequestBody ParcelRequest request) {
        return ApiResponse.ok("Parcel updated", parcelService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update parcel status and record status history")
    public ApiResponse<ParcelResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody ParcelStatusRequest request) {
        return ApiResponse.ok("Parcel status updated", parcelService.updateStatus(id, request, CurrentUser.require()));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "View parcel status history")
    public ApiResponse<List<ParcelStatusHistoryResponse>> history(@PathVariable Long id) {
        return ApiResponse.ok("Parcel history retrieved", parcelService.history(id, CurrentUser.require()));
    }
}
