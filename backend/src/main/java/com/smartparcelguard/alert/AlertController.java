package com.smartparcelguard.alert;

import com.smartparcelguard.common.ApiResponse;
import com.smartparcelguard.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "Alert history visible to the signed-in admin or operator")
    public ApiResponse<List<AlertService.AlertResponse>> list(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok("Alerts retrieved", alertService.list(CurrentUser.require(), status, unreadOnly, limit));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark an alert as read. This does not send an external notification.")
    public ApiResponse<AlertService.AlertResponse> read(@PathVariable Long id) {
        return ApiResponse.ok("Alert marked read", alertService.markRead(id, CurrentUser.require()));
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an open alert")
    public ApiResponse<AlertService.AlertResponse> acknowledge(@PathVariable Long id) {
        return ApiResponse.ok("Alert acknowledged", alertService.acknowledge(id, CurrentUser.require()));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Resolve an alert. Notes are stored with the alert.")
    public ApiResponse<AlertService.AlertResponse> resolve(@PathVariable Long id, @RequestBody(required = false) NoteRequest request) {
        String notes = request == null ? null : request.notes();
        return ApiResponse.ok("Alert resolved", alertService.resolve(id, notes, CurrentUser.require()));
    }

    public record NoteRequest(String notes) {
    }
}
