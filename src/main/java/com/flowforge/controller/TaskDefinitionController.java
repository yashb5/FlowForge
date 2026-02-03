package com.flowforge.controller;

import com.flowforge.domain.enums.TaskType;
import com.flowforge.dto.request.CreateTaskDefinitionRequest;
import com.flowforge.dto.response.ApiResponse;
import com.flowforge.dto.response.PageResponse;
import com.flowforge.dto.response.TaskDefinitionResponse;
import com.flowforge.security.CurrentUser;
import com.flowforge.service.TaskDefinitionService;
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
@RequestMapping("/api/v1/task-definitions")
@RequiredArgsConstructor
@Tag(name = "Task Definitions", description = "Task definition management endpoints")
public class TaskDefinitionController {

    private final TaskDefinitionService taskDefinitionService;

    @PostMapping
    @Operation(summary = "Create a new task definition")
    public ResponseEntity<ApiResponse<TaskDefinitionResponse>> createTaskDefinition(
            @Valid @RequestBody CreateTaskDefinitionRequest request,
            @CurrentUser UUID userId) {
        TaskDefinitionResponse taskDefinition = taskDefinitionService.createTaskDefinition(request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(taskDefinition, "Task definition created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all task definitions for current user")
    public ResponseEntity<ApiResponse<PageResponse<TaskDefinitionResponse>>> getMyTaskDefinitions(
            @CurrentUser UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TaskDefinitionResponse> taskDefinitions = taskDefinitionService.getTaskDefinitionsByOwner(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(taskDefinitions)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search task definitions")
    public ResponseEntity<ApiResponse<PageResponse<TaskDefinitionResponse>>> searchTaskDefinitions(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TaskDefinitionResponse> taskDefinitions = taskDefinitionService.searchTaskDefinitions(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(taskDefinitions)));
    }

    @GetMapping("/type/{taskType}")
    @Operation(summary = "Get task definitions by type")
    public ResponseEntity<ApiResponse<PageResponse<TaskDefinitionResponse>>> getTaskDefinitionsByType(
            @PathVariable TaskType taskType,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TaskDefinitionResponse> taskDefinitions = taskDefinitionService.getTaskDefinitionsByType(taskType, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(taskDefinitions)));
    }

    @GetMapping("/tags")
    @Operation(summary = "Get task definitions by tags")
    public ResponseEntity<ApiResponse<PageResponse<TaskDefinitionResponse>>> getTaskDefinitionsByTags(
            @RequestParam Set<String> tags,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TaskDefinitionResponse> taskDefinitions = taskDefinitionService.getTaskDefinitionsByTags(tags, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(taskDefinitions)));
    }

    @GetMapping("/my-tags")
    @Operation(summary = "Get all tags for current user's task definitions")
    public ResponseEntity<ApiResponse<Set<String>>> getMyTags(@CurrentUser UUID userId) {
        Set<String> tags = taskDefinitionService.getAllTagsForOwner(userId);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task definition by ID")
    public ResponseEntity<ApiResponse<TaskDefinitionResponse>> getTaskDefinitionById(@PathVariable UUID id) {
        TaskDefinitionResponse taskDefinition = taskDefinitionService.getTaskDefinitionById(id);
        return ResponseEntity.ok(ApiResponse.success(taskDefinition));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task definition")
    public ResponseEntity<ApiResponse<TaskDefinitionResponse>> updateTaskDefinition(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTaskDefinitionRequest request,
            @CurrentUser UUID userId) {
        TaskDefinitionResponse taskDefinition = taskDefinitionService.updateTaskDefinition(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(taskDefinition, "Task definition updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task definition")
    public ResponseEntity<ApiResponse<Void>> deleteTaskDefinition(
            @PathVariable UUID id,
            @CurrentUser UUID userId) {
        taskDefinitionService.deleteTaskDefinition(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Task definition deleted successfully"));
    }
}
