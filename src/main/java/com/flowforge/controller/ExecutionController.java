package com.flowforge.controller;

import com.flowforge.domain.enums.ExecutionStatus;
import com.flowforge.dto.response.ApiResponse;
import com.flowforge.dto.response.PageResponse;
import com.flowforge.dto.response.WorkflowExecutionResponse;
import com.flowforge.security.CurrentUser;
import com.flowforge.service.WorkflowExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
@Tag(name = "Executions", description = "Workflow execution management endpoints")
public class ExecutionController {

    private final WorkflowExecutionService workflowExecutionService;

    @GetMapping
    @Operation(summary = "Get all executions for current user")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowExecutionResponse>>> getMyExecutions(
            @CurrentUser UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WorkflowExecutionResponse> executions = workflowExecutionService.getExecutionsByOwner(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(executions)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get executions by status")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowExecutionResponse>>> getExecutionsByStatus(
            @PathVariable ExecutionStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WorkflowExecutionResponse> executions = workflowExecutionService.getExecutionsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(executions)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get execution by ID")
    public ResponseEntity<ApiResponse<WorkflowExecutionResponse>> getExecutionById(@PathVariable UUID id) {
        WorkflowExecutionResponse execution = workflowExecutionService.getExecutionById(id);
        return ResponseEntity.ok(ApiResponse.success(execution));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel execution")
    public ResponseEntity<ApiResponse<WorkflowExecutionResponse>> cancelExecution(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @CurrentUser UUID userId) {
        WorkflowExecutionResponse execution = workflowExecutionService.cancelExecution(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.success(execution, "Execution cancelled successfully"));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry failed execution")
    public ResponseEntity<ApiResponse<WorkflowExecutionResponse>> retryExecution(
            @PathVariable UUID id,
            @CurrentUser UUID userId) {
        WorkflowExecutionResponse execution = workflowExecutionService.retryExecution(id, userId);
        return ResponseEntity.ok(ApiResponse.success(execution, "Execution retry started"));
    }
}
