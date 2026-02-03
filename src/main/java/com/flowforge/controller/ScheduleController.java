package com.flowforge.controller;

import com.flowforge.dto.request.CreateScheduleRequest;
import com.flowforge.dto.response.ApiResponse;
import com.flowforge.dto.response.PageResponse;
import com.flowforge.dto.response.ScheduleResponse;
import com.flowforge.security.CurrentUser;
import com.flowforge.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows/{workflowId}/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Schedule management endpoints")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @Operation(summary = "Create a new schedule for a workflow")
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @PathVariable UUID workflowId,
            @Valid @RequestBody CreateScheduleRequest request,
            @CurrentUser UUID userId) {
        ScheduleResponse schedule = scheduleService.createSchedule(workflowId, request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(schedule, "Schedule created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all schedules for a workflow")
    public ResponseEntity<ApiResponse<PageResponse<ScheduleResponse>>> getSchedules(
            @PathVariable UUID workflowId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ScheduleResponse> schedules = scheduleService.getSchedulesByWorkflow(workflowId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(schedules)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getScheduleById(
            @PathVariable UUID workflowId,
            @PathVariable UUID id) {
        ScheduleResponse schedule = scheduleService.getScheduleByIdAndWorkflow(id, workflowId);
        return ResponseEntity.ok(ApiResponse.success(schedule));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @PathVariable UUID workflowId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateScheduleRequest request,
            @CurrentUser UUID userId) {
        ScheduleResponse schedule = scheduleService.updateSchedule(id, workflowId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(schedule, "Schedule updated successfully"));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> pauseSchedule(
            @PathVariable UUID workflowId,
            @PathVariable UUID id,
            @CurrentUser UUID userId) {
        ScheduleResponse schedule = scheduleService.pauseSchedule(id, workflowId, userId);
        return ResponseEntity.ok(ApiResponse.success(schedule, "Schedule paused successfully"));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> resumeSchedule(
            @PathVariable UUID workflowId,
            @PathVariable UUID id,
            @CurrentUser UUID userId) {
        ScheduleResponse schedule = scheduleService.resumeSchedule(id, workflowId, userId);
        return ResponseEntity.ok(ApiResponse.success(schedule, "Schedule resumed successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete schedule")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @PathVariable UUID workflowId,
            @PathVariable UUID id,
            @CurrentUser UUID userId) {
        scheduleService.deleteSchedule(id, workflowId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Schedule deleted successfully"));
    }
}
