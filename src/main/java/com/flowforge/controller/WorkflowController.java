package com.flowforge.controller;

import com.flowforge.domain.enums.WorkflowStatus;
import com.flowforge.dto.request.CreateWorkflowRequest;
import com.flowforge.dto.request.ExecuteWorkflowRequest;
import com.flowforge.dto.response.ApiResponse;
import com.flowforge.dto.response.PageResponse;
import com.flowforge.dto.response.WorkflowExecutionResponse;
import com.flowforge.dto.response.WorkflowResponse;
import com.flowforge.security.CurrentUser;
import com.flowforge.service.WorkflowExecutionService;
import com.flowforge.service.WorkflowService;
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

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflows", description = "Workflow management and execution endpoints")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowExecutionService workflowExecutionService;

    @PostMapping
    @Operation(summary = "Create a new workflow")
    public ResponseEntity<ApiResponse<WorkflowResponse>> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request,
            @CurrentUser UUID userId) {
        WorkflowResponse workflow = workflowService.createWorkflow(request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(workflow, "Workflow created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all workflows for current user")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowResponse>>> getMyWorkflows(
            @CurrentUser UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WorkflowResponse> workflows = workflowService.getWorkflowsByOwner(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(workflows)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search workflows")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowResponse>>> searchWorkflows(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WorkflowResponse> workflows = workflowService.searchWorkflows(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(workflows)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get workflows by status")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowResponse>>> getWorkflowsByStatus(
            @PathVariable WorkflowStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WorkflowResponse> workflows = workflowService.getWorkflowsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(workflows)));
    }

    @GetMapping("/tags")
    @Operation(summary = "Get workflows by tags")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowResponse>>> getWorkflowsByTags(
            @RequestParam Set<String> tags,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WorkflowResponse> workflows = workflowService.getWorkflowsByTags(tags, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(workflows)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow by ID")
    public ResponseEntity<ApiResponse<WorkflowResponse>> getWorkflowById(@PathVariable UUID id) {
        WorkflowResponse workflow = workflowService.getWorkflowById(id);
        return ResponseEntity.ok(ApiResponse.success(workflow));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update workflow")
    public ResponseEntity<ApiResponse<WorkflowResponse>> updateWorkflow(
            @PathVariable UUID id,
            @Valid @RequestBody CreateWorkflowRequest request,
            @CurrentUser UUID userId) {
        WorkflowResponse workflow = workflowService.updateWorkflow(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(workflow, "Workflow updated successfully"));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate workflow")
    public ResponseEntity<ApiResponse<WorkflowResponse>> activateWorkflow(
            @PathVariable UUID id,
            @CurrentUser UUID userId) {
        WorkflowResponse workflow = workflowService.activateWorkflow(id, userId);
        return ResponseEntity.ok(ApiResponse.success(workflow, "Workflow activated successfully"));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause workflow")
    public ResponseEntity<ApiResponse<WorkflowResponse>> pauseWorkflow(
            @PathVariable UUID id,
            @CurrentUser UUID userId) {
        WorkflowResponse workflow = workflowService.pauseWorkflow(id, userId);
        return ResponseEntity.ok(ApiResponse.success(workflow, "Workflow paused successfully"));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive workflow")
    public ResponseEntity<ApiResponse<WorkflowResponse>> archiveWorkflow(
            @PathVariable UUID id,
            @CurrentUser UUID userId) {
        WorkflowResponse workflow = workflowService.archiveWorkflow(id, userId);
        return ResponseEntity.ok(ApiResponse.success(workflow, "Workflow archived successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete workflow")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(
            @PathVariable UUID id,
            @CurrentUser UUID userId) {
        workflowService.deleteWorkflow(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Workflow deleted successfully"));
    }

    // Execution endpoints
    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute workflow")
    public ResponseEntity<ApiResponse<WorkflowExecutionResponse>> executeWorkflow(
            @PathVariable UUID id,
            @Valid @RequestBody ExecuteWorkflowRequest request,
            @CurrentUser UUID userId) {
        WorkflowExecutionResponse execution = workflowExecutionService.executeWorkflow(id, request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(execution, "Workflow execution started"));
    }

    @GetMapping("/{id}/executions")
    @Operation(summary = "Get workflow executions")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowExecutionResponse>>> getWorkflowExecutions(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WorkflowExecutionResponse> executions = workflowExecutionService.getExecutionsByWorkflow(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(executions)));
    }
}
