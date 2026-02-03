package com.flowforge.domain.entity;

import com.flowforge.domain.enums.ExecutionStatus;
import com.flowforge.domain.enums.ExecutionTrigger;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * WorkflowExecution represents a single execution instance of a workflow.
 * It tracks the overall progress, status, and results of the workflow run.
 */
@Entity
@Table(name = "workflow_executions", indexes = {
    @Index(name = "idx_wf_exec_workflow", columnList = "workflow_id"),
    @Index(name = "idx_wf_exec_status", columnList = "status"),
    @Index(name = "idx_wf_exec_started", columnList = "started_at"),
    @Index(name = "idx_wf_exec_schedule", columnList = "schedule_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    /**
     * User who triggered the execution (null for scheduled executions)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by")
    private User triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private ExecutionTrigger triggerType;

    /**
     * Input parameters for this execution (merged from workflow defaults and overrides)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> input = new HashMap<>();

    /**
     * Output results from the workflow execution
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> output = new HashMap<>();

    /**
     * Execution context - shared state between tasks
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Timeout deadline for this execution
     */
    @Column(name = "timeout_at")
    private Instant timeoutAt;

    /**
     * Error message if execution failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Stack trace if execution failed with exception
     */
    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    /**
     * Number of tasks completed successfully
     */
    @Column(name = "tasks_completed", nullable = false)
    @Builder.Default
    private Integer tasksCompleted = 0;

    /**
     * Number of tasks that failed
     */
    @Column(name = "tasks_failed", nullable = false)
    @Builder.Default
    private Integer tasksFailed = 0;

    /**
     * Total number of tasks in the workflow
     */
    @Column(name = "tasks_total", nullable = false)
    @Builder.Default
    private Integer tasksTotal = 0;

    /**
     * Whether this execution was manually cancelled
     */
    @Column(name = "is_cancelled", nullable = false)
    @Builder.Default
    private Boolean isCancelled = false;

    /**
     * User who cancelled the execution
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /**
     * Reason for cancellation
     */
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    /**
     * Correlation ID for distributed tracing
     */
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    /**
     * Parent execution ID (for sub-workflow executions)
     */
    @Column(name = "parent_execution_id")
    private UUID parentExecutionId;

    @OneToMany(mappedBy = "workflowExecution", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskExecution> taskExecutions = new ArrayList<>();

    public void addTaskExecution(TaskExecution taskExecution) {
        taskExecutions.add(taskExecution);
        taskExecution.setWorkflowExecution(this);
    }

    public Duration getDuration() {
        if (startedAt == null) return Duration.ZERO;
        Instant end = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, end);
    }

    public double getProgressPercentage() {
        if (tasksTotal == 0) return 0.0;
        return ((double) (tasksCompleted + tasksFailed) / tasksTotal) * 100;
    }

    public boolean isTimedOut() {
        return timeoutAt != null && Instant.now().isAfter(timeoutAt) && !isTerminal();
    }

    public boolean isTerminal() {
        return status == ExecutionStatus.COMPLETED || 
               status == ExecutionStatus.FAILED || 
               status == ExecutionStatus.CANCELLED ||
               status == ExecutionStatus.TIMED_OUT;
    }
}
