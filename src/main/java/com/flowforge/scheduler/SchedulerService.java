package com.flowforge.scheduler;

import com.flowforge.domain.entity.Schedule;
import com.flowforge.repository.ScheduleRepository;
import com.flowforge.service.ScheduleService;
import com.flowforge.service.WorkflowExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler service that polls for due schedules and triggers workflow executions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "flowforge.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;
    private final WorkflowExecutionService workflowExecutionService;

    @Value("${flowforge.scheduler.batch-size:100}")
    private int batchSize;

    /**
     * Poll for schedules that are due for execution.
     * Runs every 10 seconds by default.
     */
    @Scheduled(fixedDelayString = "${flowforge.scheduler.poll-interval-ms:10000}")
    @Transactional
    public void pollSchedules() {
        log.debug("Polling for due schedules...");

        try {
            Instant now = Instant.now();
            List<Schedule> dueSchedules = scheduleRepository.findReadySchedules(now);

            if (dueSchedules.isEmpty()) {
                return;
            }

            log.info("Found {} schedules due for execution", dueSchedules.size());

            int processed = 0;
            for (Schedule schedule : dueSchedules) {
                if (processed >= batchSize) {
                    log.info("Reached batch size limit of {}, remaining schedules will be processed in next poll", batchSize);
                    break;
                }

                try {
                    processSchedule(schedule);
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to process schedule {}: {}", schedule.getId(), e.getMessage(), e);
                }
            }

            log.info("Processed {} schedules", processed);
        } catch (Exception e) {
            log.error("Error during schedule polling: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single schedule and trigger workflow execution.
     */
    private void processSchedule(Schedule schedule) {
        log.info("Processing schedule: {} (workflow: {})", schedule.getName(), schedule.getWorkflow().getName());

        try {
            // Trigger the workflow execution
            workflowExecutionService.executeScheduledWorkflow(schedule.getId());

            // Update the schedule's next run time
            Instant nextRunAt = scheduleService.calculateNextRunTime(schedule);
            scheduleRepository.updateAfterExecution(schedule.getId(), Instant.now(), nextRunAt);

            log.info("Successfully triggered workflow execution for schedule: {}, next run at: {}", 
                    schedule.getId(), nextRunAt);
        } catch (Exception e) {
            log.error("Failed to execute scheduled workflow for schedule {}: {}", 
                    schedule.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check for timed out workflow executions.
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkTimeouts() {
        log.debug("Checking for timed out executions...");

        // This would be implemented to check for and handle timed out executions
        // For brevity, this is a placeholder
    }

    /**
     * Check for tasks ready to retry.
     * Runs every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkRetries() {
        log.debug("Checking for tasks ready to retry...");

        // This would be implemented to check for and process retry tasks
        // For brevity, this is a placeholder
    }

    /**
     * Cleanup completed one-time schedules.
     * Runs once per day.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOneTimeSchedules() {
        log.info("Cleaning up completed one-time schedules...");

        List<Schedule> completedSchedules = scheduleRepository.findCompletedOneTimeSchedules();
        for (Schedule schedule : completedSchedules) {
            schedule.setIsActive(false);
            scheduleRepository.save(schedule);
            log.info("Deactivated completed one-time schedule: {}", schedule.getId());
        }

        log.info("Cleaned up {} one-time schedules", completedSchedules.size());
    }
}
