package com.flowforge.service;

import com.flowforge.domain.entity.TaskDefinition;
import com.flowforge.domain.entity.User;
import com.flowforge.domain.enums.TaskType;
import com.flowforge.dto.request.CreateTaskDefinitionRequest;
import com.flowforge.dto.response.TaskDefinitionResponse;
import com.flowforge.exception.DuplicateResourceException;
import com.flowforge.exception.ResourceNotFoundException;
import com.flowforge.mapper.TaskDefinitionMapper;
import com.flowforge.repository.TaskDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskDefinitionService {

    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskDefinitionMapper taskDefinitionMapper;
    private final UserService userService;

    public TaskDefinitionResponse createTaskDefinition(CreateTaskDefinitionRequest request, UUID ownerId) {
        log.info("Creating task definition '{}' for user: {}", request.getName(), ownerId);

        User owner = userService.findUserById(ownerId);

        if (taskDefinitionRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateResourceException("TaskDefinition", "name", request.getName());
        }

        validateTaskDefinition(request);

        TaskDefinition taskDefinition = taskDefinitionMapper.toEntity(request);
        taskDefinition.setOwner(owner);

        TaskDefinition savedTaskDefinition = taskDefinitionRepository.save(taskDefinition);
        log.info("Created task definition with ID: {}", savedTaskDefinition.getId());

        return taskDefinitionMapper.toResponse(savedTaskDefinition);
    }

    @Transactional(readOnly = true)
    public TaskDefinitionResponse getTaskDefinitionById(UUID id) {
        TaskDefinition taskDefinition = findTaskDefinitionById(id);
        return taskDefinitionMapper.toResponse(taskDefinition);
    }

    @Transactional(readOnly = true)
    public TaskDefinitionResponse getTaskDefinitionByIdAndOwner(UUID id, UUID ownerId) {
        TaskDefinition taskDefinition = taskDefinitionRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskDefinition", "id", id));
        return taskDefinitionMapper.toResponse(taskDefinition);
    }

    @Transactional(readOnly = true)
    public Page<TaskDefinitionResponse> getTaskDefinitionsByOwner(UUID ownerId, Pageable pageable) {
        return taskDefinitionRepository.findByOwnerIdAndIsActiveTrue(ownerId, pageable)
                .map(taskDefinitionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskDefinitionResponse> getTaskDefinitionsByType(TaskType taskType, Pageable pageable) {
        return taskDefinitionRepository.findByTaskType(taskType, pageable)
                .map(taskDefinitionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskDefinitionResponse> searchTaskDefinitions(String search, Pageable pageable) {
        return taskDefinitionRepository.searchByNameOrDescription(search, pageable)
                .map(taskDefinitionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskDefinitionResponse> getTaskDefinitionsByTags(Set<String> tags, Pageable pageable) {
        return taskDefinitionRepository.findByTagsIn(tags, pageable)
                .map(taskDefinitionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Set<String> getAllTagsForOwner(UUID ownerId) {
        return taskDefinitionRepository.findAllTagsByOwnerId(ownerId);
    }

    public TaskDefinitionResponse updateTaskDefinition(UUID id, CreateTaskDefinitionRequest request, UUID ownerId) {
        log.info("Updating task definition: {} for user: {}", id, ownerId);

        TaskDefinition taskDefinition = taskDefinitionRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskDefinition", "id", id));

        // Check for name conflict if name is being changed
        if (!taskDefinition.getName().equals(request.getName()) &&
            taskDefinitionRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateResourceException("TaskDefinition", "name", request.getName());
        }

        validateTaskDefinition(request);
        taskDefinitionMapper.updateEntity(request, taskDefinition);

        TaskDefinition updatedTaskDefinition = taskDefinitionRepository.save(taskDefinition);
        log.info("Updated task definition: {}", updatedTaskDefinition.getId());

        return taskDefinitionMapper.toResponse(updatedTaskDefinition);
    }

    public void deleteTaskDefinition(UUID id, UUID ownerId) {
        log.info("Deleting task definition: {} for user: {}", id, ownerId);

        TaskDefinition taskDefinition = taskDefinitionRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskDefinition", "id", id));

        taskDefinition.setIsActive(false);
        taskDefinitionRepository.save(taskDefinition);

        log.info("Soft deleted task definition: {}", id);
    }

    public TaskDefinition findTaskDefinitionById(UUID id) {
        return taskDefinitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskDefinition", "id", id));
    }

    private void validateTaskDefinition(CreateTaskDefinitionRequest request) {
        // Validate HTTP-specific fields
        if (request.getTaskType() == TaskType.HTTP) {
            if (request.getHttpMethod() == null || request.getHttpMethod().isBlank()) {
                throw new IllegalArgumentException("HTTP method is required for HTTP task type");
            }
            if (!request.getHandler().startsWith("http://") && !request.getHandler().startsWith("https://")) {
                throw new IllegalArgumentException("Handler must be a valid URL for HTTP task type");
            }
        }

        // Validate SCRIPT-specific fields
        if (request.getTaskType() == TaskType.SCRIPT) {
            if (request.getHandler() == null || request.getHandler().isBlank()) {
                throw new IllegalArgumentException("Script content or path is required for SCRIPT task type");
            }
        }

        // Validate retry configuration
        if (request.getRetryDelaySeconds() != null && request.getMaxRetryDelaySeconds() != null) {
            if (request.getRetryDelaySeconds() > request.getMaxRetryDelaySeconds()) {
                throw new IllegalArgumentException("Retry delay cannot be greater than max retry delay");
            }
        }
    }
}
