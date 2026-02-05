package com.flowforge.service;

import com.flowforge.domain.entity.*;
import com.flowforge.domain.enums.*;
import com.flowforge.dto.request.ExecuteWorkflowRequest;
import com.flowforge.dto.response.WorkflowExecutionResponse;
import com.flowforge.exception.InvalidStateException;
import com.flowforge.exception.ResourceNotFoundException;
import com.flowforge.mapper.ExecutionMapper;
import com.flowforge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowExecutionService {

    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowTaskRepository workflowTaskRepository;
    private final ScheduleRepository scheduleRepository;
    private final ExecutionMapper executionMapper;
    private final UserService userService;

    public WorkflowExecutionResponse executeWorkflow(UUID workflowId, ExecuteWorkflowRequest request, UUID userId) {
        log.info("Starting workflow execution for workflow: {} by user: {}", workflowId, userId);

        Workflow workflow = workflowRepository.findByIdWithTasks(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        // Validate workflow is active
        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            throw new InvalidStateException("Cannot execute workflow with status: " + workflow.getStatus());
        }

        User triggeredBy = userService.findUserById(userId);

        // Merge input parameters
        Map<String, Object> mergedInput = new HashMap<>();
        if (workflow.getGlobalInput() != null) {
            mergedInput.putAll(workflow.getGlobalInput());
        }
        if (request.getInput() != null) {
            mergedInput.putAll(request.getInput());
        }

        // Create workflow execution
        WorkflowExecution execution = WorkflowExecution.builder()
                .workflow(workflow)
                .triggeredBy(triggeredBy)
                .triggerType(ExecutionTrigger.MANUAL)
                .status(ExecutionStatus.PENDING)
                .input(mergedInput)
                .context(new HashMap<>())
                .output(new HashMap<>())
                .tasksTotal(workflow.getTasks().size())
                .correlationId(request.getCorrelationId() != null ? 
                        request.getCorrelationId() : UUID.randomUUID().toString())
                .build();

        WorkflowExecution savedExecution = workflowExecutionRepository.save(execution);

        // Create task executions
        createTaskExecutions(savedExecution, workflow.getTasks());

        // Start execution
        startExecution(savedExecution);

        log.info("Created workflow execution: {}", savedExecution.getId());

        return getExecutionById(savedExecution.getId());
    }

    public WorkflowExecutionResponse executeScheduledWorkflow(UUID scheduleId) {
        log.info("Starting scheduled workflow execution for schedule: {}", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        if (!schedule.shouldRun()) {
            throw new InvalidStateException("Schedule is not ready to run");
        }

        Workflow workflow = schedule.getWorkflow();

        // Validate workflow is active
        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            throw new InvalidStateException("Cannot execute workflow with status: " + workflow.getStatus());
        }

        // Check concurrent executions limit
        long activeExecutions = workflowExecutionRepository.countActiveExecutionsByScheduleId(scheduleId);
        if (activeExecutions >= schedule.getMaxConcurrentExecutions()) {
            throw new InvalidStateException("Maximum concurrent executions reached for this schedule");
        }

        // Merge input parameters
        Map<String, Object> mergedInput = new HashMap<>();
        if (workflow.getGlobalInput() != null) {
            mergedInput.putAll(workflow.getGlobalInput());
        }
        if (schedule.getInputOverrides() != null) {
            mergedInput.putAll(schedule.getInputOverrides());
        }

        // Create workflow execution
        WorkflowExecution execution = WorkflowExecution.builder()
                .workflow(workflow)
                .schedule(schedule)
                .triggerType(ExecutionTrigger.SCHEDULED)
                .status(ExecutionStatus.PENDING)
                .input(mergedInput)
                .context(new HashMap<>())
                .output(new HashMap<>())
                .tasksTotal(workflow.getTasks().size())
                .correlationId(UUID.randomUUID().toString())
                .build();

        WorkflowExecution savedExecution = workflowExecutionRepository.save(execution);

        // Create task executions
        List<WorkflowTask> tasks = workflowTaskRepository.findByWorkflowIdWithDefinitions(workflow.getId());
        createTaskExecutions(savedExecution, tasks);

        // Start execution
        startExecution(savedExecution);

        log.info("Created scheduled workflow execution: {}", savedExecution.getId());

        return getExecutionById(savedExecution.getId());
    }

    @Transactional(readOnly = true)
    public WorkflowExecutionResponse getExecutionById(UUID id) {
        WorkflowExecution execution = workflowExecutionRepository.findByIdWithTaskExecutions(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowExecution", "id", id));
        
        WorkflowExecutionResponse response = executionMapper.toResponse(execution);
        response.setTaskExecutions(executionMapper.toTaskExecutionResponseList(execution.getTaskExecutions()));
        return response;
    }

    @Transactional(readOnly = true)
    public Page<WorkflowExecutionResponse> getExecutionsByWorkflow(UUID workflowId, Pageable pageable) {
        return workflowExecutionRepository.findByWorkflowId(workflowId, pageable)
                .map(executionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowExecutionResponse> getExecutionsByStatus(ExecutionStatus status, Pageable pageable) {
        return workflowExecutionRepository.findByStatus(status, pageable)
                .map(executionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkflowExecutionResponse> getExecutionsByOwner(UUID ownerId, Pageable pageable) {
        return workflowExecutionRepository.findByWorkflowOwnerId(ownerId, pageable)
                .map(executionMapper::toResponse);
    }

    public WorkflowExecutionResponse cancelExecution(UUID id, UUID userId, String reason) {
        log.info("Cancelling workflow execution: {} by user: {}", id, userId);

        WorkflowExecution execution = workflowExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowExecution", "id", id));

        if (execution.isTerminal()) {
            throw new InvalidStateException("Cannot cancel execution with status: " + execution.getStatus());
        }

        User cancelledBy = userService.findUserById(userId);

        execution.setStatus(ExecutionStatus.CANCELLED);
        execution.setIsCancelled(true);
        execution.setCancelledBy(cancelledBy);
        execution.setCancelledAt(Instant.now());
        execution.setCancellationReason(reason);
        execution.setCompletedAt(Instant.now());

        // Cancel all pending/running task executions
        List<TaskExecution> activeTasks = taskExecutionRepository.findActiveTasks(id);
        for (TaskExecution taskExecution : activeTasks) {
            taskExecution.setStatus(ExecutionStatus.CANCELLED);
            taskExecution.setCompletedAt(Instant.now());
        }
        taskExecutionRepository.saveAll(activeTasks);

        WorkflowExecution cancelledExecution = workflowExecutionRepository.save(execution);
        log.info("Cancelled workflow execution: {}", cancelledExecution.getId());

        return executionMapper.toResponse(cancelledExecution);
    }

    public WorkflowExecutionResponse retryExecution(UUID id, UUID userId) {
        log.info("Retrying workflow execution: {} by user: {}", id, userId);

        WorkflowExecution originalExecution = workflowExecutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowExecution", "id", id));

        if (originalExecution.getStatus() != ExecutionStatus.FAILED) {
            throw new InvalidStateException("Can only retry failed executions");
        }

        // Create a new execution based on the original
        ExecuteWorkflowRequest request = ExecuteWorkflowRequest.builder()
                .input(originalExecution.getInput())
                .correlationId(originalExecution.getCorrelationId() + "-retry")
                .build();

        return executeWorkflow(originalExecution.getWorkflow().getId(), request, userId);
    }

    private void createTaskExecutions(WorkflowExecution execution, List<WorkflowTask> tasks) {
        for (WorkflowTask task : tasks) {
            if (!task.getIsEnabled()) {
                continue;
            }

            TaskExecution taskExecution = TaskExecution.builder()
                    .workflowExecution(execution)
                    .workflowTask(task)
                    .status(ExecutionStatus.PENDING)
                    .attemptNumber(1)
                    .maxAttempts(task.getEffectiveMaxRetries() + 1)
                    .input(buildTaskInput(execution, task))
                    .output(new HashMap<>())
                    .metrics(new HashMap<>())
                    .queuedAt(Instant.now())
                    .build();

            execution.addTaskExecution(taskExecution);
        }

        taskExecutionRepository.saveAll(execution.getTaskExecutions());
    }

    private Map<String, Object> buildTaskInput(WorkflowExecution execution, WorkflowTask task) {
        Map<String, Object> input = new HashMap<>();

        // Start with task definition defaults
        if (task.getTaskDefinition().getDefaultInput() != null) {
            input.putAll(task.getTaskDefinition().getDefaultInput());
        }

        // Apply task-specific overrides
        if (task.getInputOverrides() != null) {
            input.putAll(task.getInputOverrides());
        }

        // Apply input mapping from execution context/input
        if (task.getInputMapping() != null) {
            for (Map.Entry<String, String> mapping : task.getInputMapping().entrySet()) {
                Object value = resolveInputMapping(mapping.getValue(), execution);
                if (value != null) {
                    input.put(mapping.getKey(), value);
                }
            }
        }

        return input;
    }

    private Object resolveInputMapping(String expression, WorkflowExecution execution) {
        // Simple JSONPath-like expression resolution
        // Format: $.input.fieldName or $.context.taskKey.output.fieldName
        if (!expression.startsWith("$.")) {
            return expression; // Literal value
        }

        String[] parts = expression.substring(2).split("\\.");
        if (parts.length < 2) {
            return null;
        }

        Map<String, Object> source;
        if ("input".equals(parts[0])) {
            source = execution.getInput();
        } else if ("context".equals(parts[0])) {
            source = execution.getContext();
        } else {
            return null;
        }

        // Navigate the path
        Object current = source;
        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                return null;
            }
        }

        return current;
    }

    private void startExecution(WorkflowExecution execution) {
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now());
        execution.setTimeoutAt(Instant.now().plusSeconds(execution.getWorkflow().getTimeoutSeconds()));
        workflowExecutionRepository.save(execution);

        // Find and start root tasks (tasks with no dependencies)
        List<TaskExecution> rootTasks = execution.getTaskExecutions().stream()
                .filter(te -> te.getWorkflowTask().getDependsOn() == null || 
                              te.getWorkflowTask().getDependsOn().isEmpty())
                .collect(Collectors.toList());

        for (TaskExecution taskExecution : rootTasks) {
            scheduleTaskExecution(taskExecution);
        }
    }

    private void scheduleTaskExecution(TaskExecution taskExecution) {
        // In a real implementation, this would push to a task queue
        // For now, just mark as scheduled
        taskExecution.setStatus(ExecutionStatus.SCHEDULED);
        taskExecutionRepository.save(taskExecution);
        
        log.info("Scheduled task execution: {} for task: {}", 
                taskExecution.getId(), taskExecution.getWorkflowTask().getTaskKey());
    }

    // Methods for the execution engine to call
    public void completeTaskExecution(UUID taskExecutionId, Map<String, Object> output) {
        TaskExecution taskExecution = taskExecutionRepository.findById(taskExecutionId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskExecution", "id", taskExecutionId));

        taskExecution.setStatus(ExecutionStatus.COMPLETED);
        taskExecution.setOutput(output);
        taskExecution.setCompletedAt(Instant.now());
        taskExecutionRepository.save(taskExecution);

        // Update workflow execution context
        WorkflowExecution workflowExecution = taskExecution.getWorkflowExecution();
        Map<String, Object> context = workflowExecution.getContext();
        Map<String, Object> taskOutput = new HashMap<>();
        taskOutput.put("output", output);
        taskOutput.put("status", "COMPLETED");
        context.put(taskExecution.getWorkflowTask().getTaskKey(), taskOutput);
        workflowExecution.setTasksCompleted(workflowExecution.getTasksCompleted() + 1);
        workflowExecutionRepository.save(workflowExecution);

        // Check if workflow is complete
        checkWorkflowCompletion(workflowExecution);

        // Schedule dependent tasks
        scheduleDependentTasks(workflowExecution, taskExecution.getWorkflowTask().getTaskKey());
    }

    public void failTaskExecution(UUID taskExecutionId, String errorMessage, String errorCode) {
        TaskExecution taskExecution = taskExecutionRepository.findById(taskExecutionId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskExecution", "id", taskExecutionId));

        if (taskExecution.canRetry()) {
            // Schedule retry
            scheduleRetry(taskExecution);
        } else {
            // Mark as failed
            taskExecution.setStatus(ExecutionStatus.FAILED);
            taskExecution.setErrorMessage(errorMessage);
            taskExecution.setErrorCode(errorCode);
            taskExecution.setCompletedAt(Instant.now());
            taskExecutionRepository.save(taskExecution);

            // Update workflow execution
            WorkflowExecution workflowExecution = taskExecution.getWorkflowExecution();
            workflowExecution.setTasksFailed(workflowExecution.getTasksFailed() + 1);

            if (!workflowExecution.getWorkflow().getContinueOnFailure()) {
                // Fail the entire workflow
                workflowExecution.setStatus(ExecutionStatus.FAILED);
                workflowExecution.setErrorMessage("Task failed: " + taskExecution.getWorkflowTask().getTaskKey());
                workflowExecution.setCompletedAt(Instant.now());
            }
            workflowExecutionRepository.save(workflowExecution);

            checkWorkflowCompletion(workflowExecution);
        }
    }

    private void scheduleRetry(TaskExecution taskExecution) {
        WorkflowTask task = taskExecution.getWorkflowTask();
        TaskDefinition taskDef = task.getTaskDefinition();

        // Calculate retry delay based on strategy
        long delaySeconds = calculateRetryDelay(taskExecution, taskDef);

        taskExecution.setStatus(ExecutionStatus.WAITING_FOR_RETRY);
        taskExecution.setAttemptNumber(taskExecution.getAttemptNumber() + 1);
        taskExecution.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
        taskExecutionRepository.save(taskExecution);

        log.info("Scheduled retry #{} for task execution: {} at {}", 
                taskExecution.getAttemptNumber(), taskExecution.getId(), taskExecution.getNextRetryAt());
    }

    private long calculateRetryDelay(TaskExecution taskExecution, TaskDefinition taskDef) {
        int attempt = taskExecution.getAttemptNumber();
        long baseDelay = taskDef.getRetryDelaySeconds();

        switch (taskDef.getRetryStrategy()) {
            case FIXED:
                return baseDelay;

            case EXPONENTIAL_BACKOFF:
                long expDelay = (long) (baseDelay * Math.pow(taskDef.getRetryMultiplier(), attempt - 1));
                return Math.min(expDelay, taskDef.getMaxRetryDelaySeconds());

            case LINEAR_BACKOFF:
                long linearDelay = baseDelay * attempt;
                return Math.min(linearDelay, taskDef.getMaxRetryDelaySeconds());

            case JITTER:
                long jitterDelay = baseDelay + (long) (Math.random() * baseDelay);
                return Math.min(jitterDelay, taskDef.getMaxRetryDelaySeconds());

            case NONE:
            default:
                return 0;
        }
    }

    private void scheduleDependentTasks(WorkflowExecution execution, String completedTaskKey) {
        for (TaskExecution taskExecution : execution.getTaskExecutions()) {
            if (taskExecution.getStatus() != ExecutionStatus.PENDING) {
                continue;
            }

            Set<String> dependencies = taskExecution.getWorkflowTask().getDependsOn();
            if (dependencies == null || dependencies.isEmpty()) {
                continue;
            }

            if (!dependencies.contains(completedTaskKey)) {
                continue;
            }

            // Check if all dependencies are satisfied
            boolean allDependenciesMet = dependencies.stream()
                    .allMatch(dep -> isTaskCompleted(execution, dep));

            if (allDependenciesMet) {
                scheduleTaskExecution(taskExecution);
            }
        }
    }

    private boolean isTaskCompleted(WorkflowExecution execution, String taskKey) {
        return execution.getTaskExecutions().stream()
                .filter(te -> te.getWorkflowTask().getTaskKey().equals(taskKey))
                .anyMatch(te -> te.getStatus() == ExecutionStatus.COMPLETED || 
                               te.getStatus() == ExecutionStatus.SKIPPED);
    }

    private void checkWorkflowCompletion(WorkflowExecution execution) {
        boolean allTasksTerminal = execution.getTaskExecutions().stream()
                .allMatch(TaskExecution::isTerminal);

        if (allTasksTerminal && !execution.isTerminal()) {
            boolean anyFailed = execution.getTaskExecutions().stream()
                    .anyMatch(te -> te.getStatus() == ExecutionStatus.FAILED);

            if (anyFailed && !execution.getWorkflow().getContinueOnFailure()) {
                execution.setStatus(ExecutionStatus.FAILED);
            } else {
                execution.setStatus(ExecutionStatus.COMPLETED);
                buildWorkflowOutput(execution);
            }
            execution.setCompletedAt(Instant.now());
            workflowExecutionRepository.save(execution);

            log.info("Workflow execution {} completed with status: {}", 
                    execution.getId(), execution.getStatus());
        }
    }

    private void buildWorkflowOutput(WorkflowExecution execution) {
        Map<String, String> outputMapping = execution.getWorkflow().getOutputMapping();
        if (outputMapping == null || outputMapping.isEmpty()) {
            return;
        }

        Map<String, Object> output = new HashMap<>();
        for (Map.Entry<String, String> mapping : outputMapping.entrySet()) {
            Object value = resolveInputMapping(mapping.getValue(), execution);
            if (value != null) {
                output.put(mapping.getKey(), value);
            }
        }
        execution.setOutput(output);
    }
}
