package com.flowforge.service;

import com.flowforge.domain.entity.*;
import com.flowforge.domain.enums.WorkflowStatus;
import com.flowforge.dto.request.CreateWorkflowRequest;
import com.flowforge.dto.response.WorkflowResponse;
import com.flowforge.exception.DuplicateResourceException;
import com.flowforge.exception.InvalidStateException;
import com.flowforge.exception.ResourceNotFoundException;
import com.flowforge.exception.ValidationException;
import com.flowforge.mapper.WorkflowMapper;
import com.flowforge.repository.TaskDefinitionRepository;
import com.flowforge.repository.WorkflowRepository;
import com.flowforge.repository.WorkflowTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowTaskRepository workflowTaskRepository;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final WorkflowMapper workflowMapper;
    private final UserService userService;

    public WorkflowResponse createWorkflow(CreateWorkflowRequest request, UUID ownerId) {
        log.info("Creating workflow '{}' for user: {}", request.getName(), ownerId);

        User owner = userService.findUserById(ownerId);

        if (workflowRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateResourceException("Workflow", "name", request.getName());
        }

        Workflow workflow = workflowMapper.toEntity(request);
        workflow.setOwner(owner);

        Workflow savedWorkflow = workflowRepository.save(workflow);

        // Create workflow tasks if provided
        if (request.getTasks() != null && !request.getTasks().isEmpty()) {
            createWorkflowTasks(savedWorkflow, request.getTasks());
        }

        log.info("Created workflow with ID: {}", savedWorkflow.getId());

        return getWorkflowById(savedWorkflow.getId());
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(UUID id) {
        Workflow workflow = workflowRepository.findByIdWithTasks(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));
        
        WorkflowResponse response = workflowMapper.toResponse(workflow);
        response.setTasks(workflowMapper.toTaskResponseList(workflow.getTasks()));
        return response;
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowByIdAndOwner(UUID id, UUID ownerId) {
        Workflow workflow = workflowRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));
        return workflowMapper.toResponse(workflow);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowResponse> getWorkflowsByOwner(UUID ownerId, Pageable pageable) {
        return workflowRepository.findByOwnerId(ownerId, pageable)
                .map(workflowMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowResponse> getWorkflowsByStatus(WorkflowStatus status, Pageable pageable) {
        return workflowRepository.findByStatus(status, pageable)
                .map(workflowMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowResponse> searchWorkflows(String search, Pageable pageable) {
        return workflowRepository.searchByNameOrDescription(search, pageable)
                .map(workflowMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowResponse> getWorkflowsByTags(Set<String> tags, Pageable pageable) {
        return workflowRepository.findByTagsIn(tags, pageable)
                .map(workflowMapper::toResponse);
    }

    public WorkflowResponse updateWorkflow(UUID id, CreateWorkflowRequest request, UUID ownerId) {
        log.info("Updating workflow: {} for user: {}", id, ownerId);

        Workflow workflow = workflowRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));

        // Don't allow updates to active workflows
        if (workflow.getStatus() == WorkflowStatus.ACTIVE) {
            throw new InvalidStateException("Cannot update an active workflow. Please pause it first.");
        }

        // Check for name conflict if name is being changed
        if (!workflow.getName().equals(request.getName()) &&
            workflowRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateResourceException("Workflow", "name", request.getName());
        }

        workflowMapper.updateEntity(request, workflow);

        // Update tasks if provided
        if (request.getTasks() != null) {
            workflowTaskRepository.deleteByWorkflowId(workflow.getId());
            workflow.getTasks().clear();
            createWorkflowTasks(workflow, request.getTasks());
        }

        Workflow updatedWorkflow = workflowRepository.save(workflow);
        log.info("Updated workflow: {}", updatedWorkflow.getId());

        return getWorkflowById(updatedWorkflow.getId());
    }

    public WorkflowResponse activateWorkflow(UUID id, UUID ownerId) {
        log.info("Activating workflow: {}", id);

        Workflow workflow = workflowRepository.findByIdWithTasks(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));

        if (!workflow.getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Workflow", "id", id);
        }

        // Validate workflow before activation
        validateWorkflowForActivation(workflow);

        workflow.setStatus(WorkflowStatus.ACTIVE);
        Workflow activatedWorkflow = workflowRepository.save(workflow);

        log.info("Activated workflow: {}", activatedWorkflow.getId());
        return workflowMapper.toResponse(activatedWorkflow);
    }

    public WorkflowResponse pauseWorkflow(UUID id, UUID ownerId) {
        log.info("Pausing workflow: {}", id);

        Workflow workflow = workflowRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));

        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            throw new InvalidStateException("Only active workflows can be paused");
        }

        workflow.setStatus(WorkflowStatus.PAUSED);
        Workflow pausedWorkflow = workflowRepository.save(workflow);

        log.info("Paused workflow: {}", pausedWorkflow.getId());
        return workflowMapper.toResponse(pausedWorkflow);
    }

    public WorkflowResponse archiveWorkflow(UUID id, UUID ownerId) {
        log.info("Archiving workflow: {}", id);

        Workflow workflow = workflowRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));

        workflow.setStatus(WorkflowStatus.ARCHIVED);
        workflow.setIsActive(false);
        Workflow archivedWorkflow = workflowRepository.save(workflow);

        log.info("Archived workflow: {}", archivedWorkflow.getId());
        return workflowMapper.toResponse(archivedWorkflow);
    }

    public void deleteWorkflow(UUID id, UUID ownerId) {
        log.info("Deleting workflow: {} for user: {}", id, ownerId);

        Workflow workflow = workflowRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));

        workflow.setIsActive(false);
        workflow.setStatus(WorkflowStatus.ARCHIVED);
        workflowRepository.save(workflow);

        log.info("Soft deleted workflow: {}", id);
    }

    public Workflow findWorkflowById(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));
    }

    private void createWorkflowTasks(Workflow workflow, List<CreateWorkflowRequest.WorkflowTaskRequest> taskRequests) {
        // Collect all task definition IDs
        Set<UUID> taskDefIds = taskRequests.stream()
                .map(CreateWorkflowRequest.WorkflowTaskRequest::getTaskDefinitionId)
                .collect(Collectors.toSet());

        // Fetch all task definitions at once
        List<TaskDefinition> taskDefinitions = taskDefinitionRepository.findAllActiveByIds(taskDefIds);
        Map<UUID, TaskDefinition> taskDefMap = taskDefinitions.stream()
                .collect(Collectors.toMap(TaskDefinition::getId, td -> td));

        // Validate all task definitions exist
        for (UUID taskDefId : taskDefIds) {
            if (!taskDefMap.containsKey(taskDefId)) {
                throw new ResourceNotFoundException("TaskDefinition", "id", taskDefId);
            }
        }

        // Collect all task keys for dependency validation
        Set<String> taskKeys = taskRequests.stream()
                .map(CreateWorkflowRequest.WorkflowTaskRequest::getTaskKey)
                .collect(Collectors.toSet());

        // Create workflow tasks
        for (CreateWorkflowRequest.WorkflowTaskRequest taskRequest : taskRequests) {
            // Validate dependencies
            if (taskRequest.getDependsOn() != null) {
                for (String dep : taskRequest.getDependsOn()) {
                    if (!taskKeys.contains(dep)) {
                        throw new ValidationException("Invalid dependency: task key '" + dep + "' does not exist in workflow");
                    }
                    if (dep.equals(taskRequest.getTaskKey())) {
                        throw new ValidationException("Task cannot depend on itself: " + dep);
                    }
                }
            }

            TaskDefinition taskDef = taskDefMap.get(taskRequest.getTaskDefinitionId());

            WorkflowTask workflowTask = WorkflowTask.builder()
                    .workflow(workflow)
                    .taskDefinition(taskDef)
                    .taskKey(taskRequest.getTaskKey())
                    .name(taskRequest.getName())
                    .executionOrder(taskRequest.getExecutionOrder() != null ? taskRequest.getExecutionOrder() : 0)
                    .inputOverrides(taskRequest.getInputOverrides() != null ? taskRequest.getInputOverrides() : new HashMap<>())
                    .inputMapping(taskRequest.getInputMapping() != null ? taskRequest.getInputMapping() : new HashMap<>())
                    .conditionExpression(taskRequest.getConditionExpression())
                    .isEnabled(taskRequest.getIsEnabled() != null ? taskRequest.getIsEnabled() : true)
                    .timeoutOverrideSeconds(taskRequest.getTimeoutOverrideSeconds())
                    .maxRetriesOverride(taskRequest.getMaxRetriesOverride())
                    .dependsOn(taskRequest.getDependsOn() != null ? taskRequest.getDependsOn() : new HashSet<>())
                    .build();

            workflow.addTask(workflowTask);
        }

        // Validate no circular dependencies
        validateNoCyclicDependencies(workflow.getTasks());
    }

    private void validateWorkflowForActivation(Workflow workflow) {
        List<String> errors = new ArrayList<>();

        if (workflow.getTasks() == null || workflow.getTasks().isEmpty()) {
            errors.add("Workflow must have at least one task");
        }

        // Check for orphan dependencies
        if (workflow.getTasks() != null) {
            Set<String> taskKeys = workflow.getTasks().stream()
                    .map(WorkflowTask::getTaskKey)
                    .collect(Collectors.toSet());

            for (WorkflowTask task : workflow.getTasks()) {
                if (task.getDependsOn() != null) {
                    for (String dep : task.getDependsOn()) {
                        if (!taskKeys.contains(dep)) {
                            errors.add("Task '" + task.getTaskKey() + "' has invalid dependency: " + dep);
                        }
                    }
                }

                // Validate task definition is active
                if (!task.getTaskDefinition().getIsActive()) {
                    errors.add("Task '" + task.getTaskKey() + "' uses inactive task definition");
                }
            }

            // Check for cyclic dependencies
            try {
                validateNoCyclicDependencies(workflow.getTasks());
            } catch (ValidationException e) {
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Workflow validation failed: " + String.join("; ", errors));
        }
    }

    private void validateNoCyclicDependencies(List<WorkflowTask> tasks) {
        Map<String, Set<String>> graph = new HashMap<>();
        
        for (WorkflowTask task : tasks) {
            graph.put(task.getTaskKey(), task.getDependsOn() != null ? task.getDependsOn() : new HashSet<>());
        }

        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String taskKey : graph.keySet()) {
            if (hasCycle(taskKey, graph, visited, recursionStack)) {
                throw new ValidationException("Cyclic dependency detected involving task: " + taskKey);
            }
        }
    }

    private boolean hasCycle(String node, Map<String, Set<String>> graph, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(node)) {
            return true;
        }

        if (visited.contains(node)) {
            return false;
        }

        visited.add(node);
        recursionStack.add(node);

        Set<String> neighbors = graph.getOrDefault(node, Collections.emptySet());
        for (String neighbor : neighbors) {
            if (hasCycle(neighbor, graph, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(node);
        return false;
    }
}
