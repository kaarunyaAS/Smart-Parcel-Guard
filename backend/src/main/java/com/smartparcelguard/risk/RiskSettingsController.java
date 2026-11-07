package com.smartparcelguard.risk;

import com.smartparcelguard.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/risk-settings")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Risk settings")
public class RiskSettingsController {

    private final RiskSettingsService riskSettingsService;

    public RiskSettingsController(RiskSettingsService riskSettingsService) {
        this.riskSettingsService = riskSettingsService;
    }

    @GetMapping
    @Operation(summary = "Retrieve risk and telemetry thresholds")
    public ApiResponse<RiskSettings> get() {
        return ApiResponse.ok("Risk settings retrieved", riskSettingsService.get());
    }

    @PutMapping
    @Operation(summary = "Update risk and telemetry thresholds")
    public ApiResponse<RiskSettings> update(@Valid @RequestBody RiskSettings settings) {
        return ApiResponse.ok("Risk settings updated", riskSettingsService.update(settings));
    }
}
