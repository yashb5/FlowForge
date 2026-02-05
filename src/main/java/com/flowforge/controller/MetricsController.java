package com.flowforge.controller;

import com.flowforge.domain.enums.Granularity;
import com.flowforge.dto.response.ApiResponse;
import com.flowforge.dto.response.ExecutionTrendResponse;
import com.flowforge.dto.response.SystemMetricsResponse;
import com.flowforge.dto.response.WorkflowMetricsResponse;
import com.flowforge.exception.ValidationException;
import com.flowforge.security.CurrentUser;
import com.flowforge.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Validated
@Tag(name = "Metrics", description = "Metrics and observability endpoints for dashboard")
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping
    @Operation(summary = "Get system-wide metrics", description = "Returns counts, success rates for users, workflows, executions")
    public ResponseEntity<ApiResponse<SystemMetricsResponse>> getSystemMetrics(@CurrentUser UUID userId) {
        SystemMetricsResponse metrics = metricsService.getSystemMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/workflows/{workflowId}")
    @Operation(summary = "Get per-workflow metrics", description = "Execution stats, task-level metrics, daily trends for specific workflow")
    public ResponseEntity<ApiResponse<WorkflowMetricsResponse>> getWorkflowMetrics(
            @PathVariable UUID workflowId,
            @CurrentUser UUID userId) {
        WorkflowMetricsResponse metrics = metricsService.getWorkflowMetrics(workflowId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get execution trends", description = "Trends with configurable granularity (hourly/daily/weekly) and optional days lookback")
    public ResponseEntity<ApiResponse<ExecutionTrendResponse>> getExecutionTrends(
            @RequestParam(defaultValue = "daily")
            @Parameter(description = "Granularity for trends")
            @Pattern(regexp = "^(hourly|daily|weekly)$", message = "Granularity must be one of: hourly, daily, weekly")
            String granularityStr,
            @RequestParam(defaultValue = "30")
            @Parameter(description = "Number of days for trend lookback")
            @Min(value = 1, message = "Days must be at least 1")
            @Max(value = 90, message = "Days must not exceed 90")
            Integer days,
            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number (0-based)")
            @Min(value = 0, message = "Page must be at least 0")
            Integer page,
            @RequestParam(defaultValue = "20")
            @Parameter(description = "Page size")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            Integer size,
            @CurrentUser UUID userId) {
        // Convert to enum (case-insensitive match via pattern)
        Granularity granularity = Granularity.valueOf(granularityStr.toUpperCase());
        ExecutionTrendResponse trends = metricsService.getExecutionTrends(granularity, days, page, size);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }
}
