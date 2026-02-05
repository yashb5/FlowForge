package com.flowforge.config;

import com.flowforge.repository.WorkflowExecutionRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class WorkflowEngineHealthIndicator implements HealthIndicator {

    private final WorkflowExecutionRepository workflowExecutionRepository;

    public WorkflowEngineHealthIndicator(WorkflowExecutionRepository workflowExecutionRepository) {
        this.workflowExecutionRepository = workflowExecutionRepository;
    }

    @Override
    public Health health() {
        try {
            long active = workflowExecutionRepository.countByStatus(com.flowforge.domain.enums.ExecutionStatus.RUNNING);
            return Health.up()
                    .withDetail("active_executions", active)
                    .withDetail("engine_status", "operational")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
