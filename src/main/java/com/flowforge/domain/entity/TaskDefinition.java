package com.flowforge.domain.entity;

import com.flowforge.domain.enums.TaskType;
import com.flowforge.domain.enums.RetryStrategy;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TaskDefinition represents a reusable task template that can be used in workflows.
 * It defines the task's behavior, retry policy, timeout settings, and other configurations.
 */
@Entity
@Table(name = "task_definitions", indexes = {
    @Index(name = "idx_task_def_name", columnList = "name"),
    @Index(name = "idx_task_def_owner", columnList = "owner_id"),
    @Index(name = "idx_task_def_type", columnList = "task_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDefinition extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Handler class or endpoint that executes this task.
     * For HTTP tasks: the URL to call
     * For SCRIPT tasks: the script path or inline script
     * For QUEUE tasks: the queue name
     */
    @Column(name = "handler", nullable = false, columnDefinition = "TEXT")
    private String handler;

    /**
     * HTTP method for HTTP tasks (GET, POST, PUT, DELETE, PATCH)
     */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /**
     * Default headers for HTTP tasks, stored as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_headers", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> defaultHeaders = new HashMap<>();

    /**
     * Default input parameters for the task, stored as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_input", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> defaultInput = new HashMap<>();

    /**
     * JSON Schema for validating task input
     */
    @Column(name = "input_schema", columnDefinition = "TEXT")
    private String inputSchema;

    /**
     * JSON Schema for validating task output
     */
    @Column(name = "output_schema", columnDefinition = "TEXT")
    private String outputSchema;

    // Retry Configuration
    @Enumerated(EnumType.STRING)
    @Column(name = "retry_strategy", nullable = false)
    @Builder.Default
    private RetryStrategy retryStrategy = RetryStrategy.EXPONENTIAL_BACKOFF;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "retry_delay_seconds", nullable = false)
    @Builder.Default
    private Integer retryDelaySeconds = 60;

    @Column(name = "retry_multiplier")
    @Builder.Default
    private Double retryMultiplier = 2.0;

    @Column(name = "max_retry_delay_seconds")
    @Builder.Default
    private Integer maxRetryDelaySeconds = 3600;

    // Timeout Configuration
    @Column(name = "timeout_seconds", nullable = false)
    @Builder.Default
    private Integer timeoutSeconds = 300;

    // Priority (higher = more important)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 5;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Tags for categorization and filtering
     */
    @ElementCollection
    @CollectionTable(name = "task_definition_tags", joinColumns = @JoinColumn(name = "task_definition_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "taskDefinition", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<WorkflowTask> workflowTasks = new HashSet<>();
}
