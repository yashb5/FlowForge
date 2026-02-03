package com.flowforge.domain.entity;

import com.flowforge.domain.enums.ScheduleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Schedule defines when and how often a workflow should be executed.
 * Supports cron expressions, fixed intervals, and one-time executions.
 */
@Entity
@Table(name = "schedules", indexes = {
    @Index(name = "idx_schedule_workflow", columnList = "workflow_id"),
    @Index(name = "idx_schedule_next_run", columnList = "next_run_at"),
    @Index(name = "idx_schedule_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;

    /**
     * Cron expression for CRON schedule type
     * Format: "seconds minutes hours day-of-month month day-of-week"
     */
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    /**
     * Interval in seconds for INTERVAL schedule type
     */
    @Column(name = "interval_seconds")
    private Long intervalSeconds;

    /**
     * Specific execution time for ONE_TIME schedule type
     */
    @Column(name = "run_at")
    private Instant runAt;

    /**
     * Timezone for cron expression evaluation
     */
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";

    /**
     * When the schedule becomes active
     */
    @Column(name = "start_at")
    private Instant startAt;

    /**
     * When the schedule expires (null = never)
     */
    @Column(name = "end_at")
    private Instant endAt;

    /**
     * Next scheduled execution time
     */
    @Column(name = "next_run_at")
    private Instant nextRunAt;

    /**
     * Last execution time
     */
    @Column(name = "last_run_at")
    private Instant lastRunAt;

    /**
     * Override input parameters for scheduled executions
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_overrides", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> inputOverrides = new HashMap<>();

    /**
     * Maximum number of executions (null = unlimited)
     */
    @Column(name = "max_executions")
    private Integer maxExecutions;

    /**
     * Current execution count
     */
    @Column(name = "execution_count", nullable = false)
    @Builder.Default
    private Integer executionCount = 0;

    /**
     * Maximum concurrent executions from this schedule
     */
    @Column(name = "max_concurrent_executions", nullable = false)
    @Builder.Default
    private Integer maxConcurrentExecutions = 1;

    /**
     * Whether to catch up missed executions
     */
    @Column(name = "catch_up_missed", nullable = false)
    @Builder.Default
    private Boolean catchUpMissed = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_paused", nullable = false)
    @Builder.Default
    private Boolean isPaused = false;

    /**
     * Tags for categorization
     */
    @ElementCollection
    @CollectionTable(name = "schedule_tags", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<WorkflowExecution> executions = new HashSet<>();

    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }

    public boolean isExpired() {
        if (endAt == null) return false;
        return Instant.now().isAfter(endAt);
    }

    public boolean hasReachedMaxExecutions() {
        if (maxExecutions == null) return false;
        return executionCount >= maxExecutions;
    }

    public boolean shouldRun() {
        return isActive && !isPaused && !isExpired() && !hasReachedMaxExecutions();
    }
}
