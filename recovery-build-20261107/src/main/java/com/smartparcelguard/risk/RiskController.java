package com.smartparcelguard.risk;

import com.smartparcelguard.common.ApiResponse;
import com.smartparcelguard.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/parcels/{parcelId}/risk")
@Tag(name = "Risk")
public class RiskController {

    private final RiskAnalysisService riskAnalysisService;

    public RiskController(RiskAnalysisService riskAnalysisService) {
        this.riskAnalysisService = riskAnalysisService;
    }

    @GetMapping
    @Operation(summary = "Latest stored risk score, severity, and reasons")
    public ApiResponse<RiskAnalysisService.RiskView> latest(@PathVariable Long parcelId) {
        return ApiResponse.ok("Risk retrieved", riskAnalysisService.latest(parcelId, CurrentUser.require()));
    }

    @GetMapping("/history")
    @Operation(summary = "Risk score history")
    public ApiResponse<List<RiskAnalysisService.RiskView>> history(@PathVariable Long parcelId) {
        return ApiResponse.ok("Risk history retrieved", riskAnalysisService.history(parcelId, CurrentUser.require()));
    }

    @PostMapping("/recalculate")
    @Operation(summary = "Recalculate risk from stored telemetry")
    public ApiResponse<RiskAnalysisService.RiskView> recalculate(@PathVariable Long parcelId) {
        return ApiResponse.ok("Risk recalculated", riskAnalysisService.recalculateFor(parcelId, CurrentUser.require()));
    }
}
