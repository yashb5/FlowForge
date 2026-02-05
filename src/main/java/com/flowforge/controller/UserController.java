package com.flowforge.controller;

import com.flowforge.domain.enums.UserRole;
import com.flowforge.dto.request.UpdateUserRequest;
import com.flowforge.dto.response.ApiResponse;
import com.flowforge.dto.response.PageResponse;
import com.flowforge.dto.response.UserResponse;
import com.flowforge.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(users)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users (admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserResponse> users = userService.searchUsers(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(users)));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get users by role (admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsersByRole(
            @PathVariable UserRole role,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserResponse> users = userService.getUsersByRole(role, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(users)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#id)")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#id)")
    @Operation(summary = "Update user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user (admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }
}
