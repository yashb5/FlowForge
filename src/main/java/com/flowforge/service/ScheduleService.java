package com.flowforge.service;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.flowforge.domain.entity.Schedule;
import com.flowforge.domain.entity.Workflow;
import com.flowforge.domain.enums.ScheduleType;
import com.flowforge.domain.enums.WorkflowStatus;
import com.flowforge.dto.request.CreateScheduleRequest;
import com.flowforge.dto.response.ScheduleResponse;
import com.flowforge.exception.DuplicateResourceException;
import com.flowforge.exception.InvalidStateException;
import com.flowforge.exception.ResourceNotFoundException;
import com.flowforge.exception.ValidationException;
import com.flowforge.mapper.ScheduleMapper;
import com.flowforge.repository.ScheduleRepository;
import com.flowforge.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final WorkflowRepository workflowRepository;
    private final ScheduleMapper scheduleMapper;

    private final CronParser cronParser = new CronParser(
            CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    );

    public ScheduleResponse createSchedule(UUID workflowId, CreateScheduleRequest request, UUID ownerId) {
        log.info("Creating schedule '{}' for workflow: {}", request.getName(), workflowId);

        Workflow workflow = workflowRepository.findByIdAndOwnerId(workflowId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        if (scheduleRepository.existsByNameAndWorkflowId(request.getName(), workflowId)) {
            throw new DuplicateResourceException("Schedule", "name", request.getName());
        }

        validateScheduleRequest(request);

        Schedule schedule = scheduleMapper.toEntity(request);
        schedule.setWorkflow(workflow);

        // Calculate next run time
        Instant nextRunAt = calculateNextRunTime(schedule);
        schedule.setNextRunAt(nextRunAt);

        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.info("Created schedule with ID: {}", savedSchedule.getId());

        return scheduleMapper.toResponse(savedSchedule);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getScheduleById(UUID id) {
        Schedule schedule = findScheduleById(id);
        return scheduleMapper.toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getScheduleByIdAndWorkflow(UUID id, UUID workflowId) {
        Schedule schedule = scheduleRepository.findByIdAndWorkflowId(id, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));
        return scheduleMapper.toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public Page<ScheduleResponse> getSchedulesByWorkflow(UUID workflowId, Pageable pageable) {
        return scheduleRepository.findByWorkflowId(workflowId, pageable)
                .map(scheduleMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ScheduleResponse> getSchedulesByOwner(UUID ownerId, Pageable pageable) {
        return scheduleRepository.findByWorkflowOwnerId(ownerId, pageable)
                .map(scheduleMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ScheduleResponse> getActiveSchedules(Pageable pageable) {
        return scheduleRepository.findByIsActiveTrueAndIsPausedFalse(pageable)
                .map(scheduleMapper::toResponse);
    }

    public ScheduleResponse updateSchedule(UUID id, UUID workflowId, CreateScheduleRequest request, UUID ownerId) {
        log.info("Updating schedule: {} for workflow: {}", id, workflowId);

        // Verify workflow ownership
        workflowRepository.findByIdAndOwnerId(workflowId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        Schedule schedule = scheduleRepository.findByIdAndWorkflowId(id, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));

        // Check for name conflict if name is being changed
        if (!schedule.getName().equals(request.getName()) &&
            scheduleRepository.existsByNameAndWorkflowId(request.getName(), workflowId)) {
            throw new DuplicateResourceException("Schedule", "name", request.getName());
        }

        validateScheduleRequest(request);
        scheduleMapper.updateEntity(request, schedule);

        // Recalculate next run time
        Instant nextRunAt = calculateNextRunTime(schedule);
        schedule.setNextRunAt(nextRunAt);

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.info("Updated schedule: {}", updatedSchedule.getId());

        return scheduleMapper.toResponse(updatedSchedule);
    }

    public ScheduleResponse pauseSchedule(UUID id, UUID workflowId, UUID ownerId) {
        log.info("Pausing schedule: {}", id);

        // Verify workflow ownership
        workflowRepository.findByIdAndOwnerId(workflowId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        Schedule schedule = scheduleRepository.findByIdAndWorkflowId(id, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));

        schedule.setIsPaused(true);
        Schedule pausedSchedule = scheduleRepository.save(schedule);

        log.info("Paused schedule: {}", pausedSchedule.getId());
        return scheduleMapper.toResponse(pausedSchedule);
    }

    public ScheduleResponse resumeSchedule(UUID id, UUID workflowId, UUID ownerId) {
        log.info("Resuming schedule: {}", id);

        Workflow workflow = workflowRepository.findByIdAndOwnerId(workflowId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        Schedule schedule = scheduleRepository.findByIdAndWorkflowId(id, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));

        // Verify workflow is active
        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            throw new InvalidStateException("Cannot resume schedule for non-active workflow");
        }

        schedule.setIsPaused(false);
        
        // Recalculate next run time
        Instant nextRunAt = calculateNextRunTime(schedule);
        schedule.setNextRunAt(nextRunAt);

        Schedule resumedSchedule = scheduleRepository.save(schedule);

        log.info("Resumed schedule: {}", resumedSchedule.getId());
        return scheduleMapper.toResponse(resumedSchedule);
    }

    public void deleteSchedule(UUID id, UUID workflowId, UUID ownerId) {
        log.info("Deleting schedule: {} for workflow: {}", id, workflowId);

        // Verify workflow ownership
        workflowRepository.findByIdAndOwnerId(workflowId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        Schedule schedule = scheduleRepository.findByIdAndWorkflowId(id, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));

        schedule.setIsActive(false);
        scheduleRepository.save(schedule);

        log.info("Soft deleted schedule: {}", id);
    }

    public Schedule findScheduleById(UUID id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));
    }

    public Instant calculateNextRunTime(Schedule schedule) {
        Instant now = Instant.now();
        ZoneId zoneId = schedule.getZoneId();

        switch (schedule.getScheduleType()) {
            case CRON:
                Cron cron = cronParser.parse(schedule.getCronExpression());
                ExecutionTime executionTime = ExecutionTime.forCron(cron);
                ZonedDateTime zonedNow = ZonedDateTime.ofInstant(now, zoneId);
                Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(zonedNow);
                return nextExecution.map(ZonedDateTime::toInstant).orElse(null);

            case INTERVAL:
                if (schedule.getLastRunAt() != null) {
                    return schedule.getLastRunAt().plusSeconds(schedule.getIntervalSeconds());
                }
                return schedule.getStartAt() != null ? schedule.getStartAt() : now;

            case ONE_TIME:
                if (schedule.getExecutionCount() > 0) {
                    return null; // Already executed
                }
                return schedule.getRunAt();

            case EVENT_DRIVEN:
                return null; // No scheduled next run

            default:
                return null;
        }
    }

    private void validateScheduleRequest(CreateScheduleRequest request) {
        switch (request.getScheduleType()) {
            case CRON:
                if (request.getCronExpression() == null || request.getCronExpression().isBlank()) {
                    throw new ValidationException("Cron expression is required for CRON schedule type");
                }
                try {
                    cronParser.parse(request.getCronExpression());
                } catch (Exception e) {
                    throw new ValidationException("Invalid cron expression: " + e.getMessage());
                }
                break;

            case INTERVAL:
                if (request.getIntervalSeconds() == null || request.getIntervalSeconds() < 60) {
                    throw new ValidationException("Interval must be at least 60 seconds for INTERVAL schedule type");
                }
                break;

            case ONE_TIME:
                if (request.getRunAt() == null) {
                    throw new ValidationException("Run at time is required for ONE_TIME schedule type");
                }
                if (request.getRunAt().isBefore(Instant.now())) {
                    throw new ValidationException("Run at time must be in the future");
                }
                break;

            case EVENT_DRIVEN:
                // No specific validation needed
                break;
        }

        // Validate timezone
        try {
            ZoneId.of(request.getTimezone());
        } catch (Exception e) {
            throw new ValidationException("Invalid timezone: " + request.getTimezone());
        }

        // Validate date range
        if (request.getStartAt() != null && request.getEndAt() != null) {
            if (request.getEndAt().isBefore(request.getStartAt())) {
                throw new ValidationException("End time must be after start time");
            }
        }
    }
}
