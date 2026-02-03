package com.flowforge.domain.entity;

import com.flowforge.domain.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * TaskExecution represents a single execution of a task within a workflow execution.
 * It tracks the task's progress, retries, input/output, and timing.
 */
@Entity
@Table(name = "task_executions", indexes = {
    @Index(name = "idx_task_exec_wf_exec", columnList = "workflow_execution_id"),
    @Index(name = "idx_task_exec_wf_task", columnList = "workflow_task_id"),
    @Index(name = "idx_task_exec_status", columnList = "status"),
    @Index(name = "idx_task_exec_started", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_execution_id", nullable = false)
    private WorkflowExecution workflowExecution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_task_id", nullable = false)
    private WorkflowTask workflowTask;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    /**
     * Attempt number (1 = first attempt, 2 = first retry, etc.)
     */
    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    /**
     * Maximum attempts allowed for this execution
     */
    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 1;

    /**
     * Input parameters for this execution
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> input = new HashMap<>();

    /**
     * Output from this execution
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> output = new HashMap<>();

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * When the next retry should be attempted
     */
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

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
     * Error code for programmatic error handling
     */
    @Column(name = "error_code", length = 100)
    private String errorCode;

    /**
     * Stack trace if execution failed with exception
     */
    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    /**
     * HTTP status code for HTTP task types
     */
    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    /**
     * Raw response body (truncated if too large)
     */
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    /**
     * Worker/node that executed this task
     */
    @Column(name = "worker_id", length = 100)
    private String workerId;

    /**
     * Log output from the task execution
     */
    @Column(name = "logs", columnDefinition = "TEXT")
    private String logs;

    /**
     * Metrics collected during execution
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metrics = new HashMap<>();

    /**
     * Whether the condition expression was evaluated and passed
     */
    @Column(name = "condition_evaluated")
    private Boolean conditionEvaluated;

    /**
     * Whether this task was skipped due to condition
     */
    @Column(name = "is_skipped", nullable = false)
    @Builder.Default
    private Boolean isSkipped = false;

    public Duration getQueueDuration() {
        if (queuedAt == null || startedAt == null) return Duration.ZERO;
        return Duration.between(queuedAt, startedAt);
    }

    public Duration getExecutionDuration() {
        if (startedAt == null) return Duration.ZERO;
        Instant end = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, end);
    }

    public Duration getTotalDuration() {
        if (queuedAt == null) return Duration.ZERO;
        Instant end = completedAt != null ? completedAt : Instant.now();
        return Duration.between(queuedAt, end);
    }

    public boolean canRetry() {
        return attemptNumber < maxAttempts && 
               (status == ExecutionStatus.FAILED || status == ExecutionStatus.TIMED_OUT);
    }

    public boolean isTimedOut() {
        return timeoutAt != null && Instant.now().isAfter(timeoutAt) && 
               status == ExecutionStatus.RUNNING;
    }

    public boolean isTerminal() {
        return status == ExecutionStatus.COMPLETED || 
               status == ExecutionStatus.FAILED || 
               status == ExecutionStatus.CANCELLED ||
               status == ExecutionStatus.SKIPPED;
    }
}
