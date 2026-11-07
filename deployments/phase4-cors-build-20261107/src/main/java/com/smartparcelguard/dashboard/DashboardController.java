package com.smartparcelguard.dashboard;

import com.smartparcelguard.common.ApiResponse;
import com.smartparcelguard.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Parcel, device, alert, and risk totals from stored records")
    public ApiResponse<DashboardService.DashboardResponse> summary() {
        return ApiResponse.ok("Dashboard retrieved", dashboardService.summary(CurrentUser.require()));
    }
}
