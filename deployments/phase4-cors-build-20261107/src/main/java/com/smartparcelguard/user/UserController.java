package com.smartparcelguard.user;

import com.smartparcelguard.common.ApiResponse;
import com.smartparcelguard.common.PageResponse;
import com.smartparcelguard.user.dto.CreateUserRequest;
import com.smartparcelguard.user.dto.UpdateUserRequest;
import com.smartparcelguard.user.dto.UserResponse;
import com.smartparcelguard.user.dto.UserStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create a user and assign a role")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created", userService.create(request)));
    }

    @GetMapping
    @Operation(summary = "List users")
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.ok("Users retrieved", userService.list(page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "View a user")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok("User retrieved", userService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user, including role assignment")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok("User updated", userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a user")
    public ApiResponse<UserResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.ok("User status updated", userService.updateStatus(id, request));
    }
}
