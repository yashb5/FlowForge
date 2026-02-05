package com.flowforge.config;

import com.flowforge.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SchedulerHealthIndicator implements HealthIndicator {

    private final ScheduleRepository scheduleRepository;

    @Value("${flowforge.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    public SchedulerHealthIndicator(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public Health health() {
        try {
            long activeSchedules = scheduleRepository.countActiveSchedules();
            String status = schedulerEnabled ? "running" : "disabled";
            return Health.up()
                    .withDetail("active_schedules", activeSchedules)
                    .withDetail("scheduler_enabled", schedulerEnabled)
                    .withDetail("status", status)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
