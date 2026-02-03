package com.flowforge.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

/**
 * WorkflowTask represents a task instance within a workflow.
 * It defines the task's position in the DAG and its dependencies.
 */
@Entity
@Table(name = "workflow_tasks", indexes = {
    @Index(name = "idx_wf_task_workflow", columnList = "workflow_id"),
    @Index(name = "idx_wf_task_def", columnList = "task_definition_id"),
    @Index(name = "idx_wf_task_order", columnList = "execution_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_definition_id", nullable = false)
    private TaskDefinition taskDefinition;

    /**
     * Unique identifier for this task within the workflow (used for referencing in dependencies)
     */
    @Column(name = "task_key", nullable = false, length = 100)
    private String taskKey;

    /**
     * Display name for this task instance (can override task definition name)
     */
    @Column(name = "name", length = 255)
    private String name;

    /**
     * Execution order - tasks with lower numbers execute first (when dependencies allow)
     */
    @Column(name = "execution_order", nullable = false)
    @Builder.Default
    private Integer executionOrder = 0;

    /**
     * Input parameter overrides for this specific task instance
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_overrides", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> inputOverrides = new HashMap<>();

    /**
     * Input mapping - maps workflow context/previous task outputs to this task's input
     * Format: { "taskInputParam": "$.previousTask.output.field" }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_mapping", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> inputMapping = new HashMap<>();

    /**
     * Condition expression - task only executes if this evaluates to true
     * Supports JSONPath expressions against workflow context
     */
    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String conditionExpression;

    /**
     * Whether this task is enabled in the workflow
     */
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * Override timeout for this specific task instance
     */
    @Column(name = "timeout_override_seconds")
    private Integer timeoutOverrideSeconds;

    /**
     * Override max retries for this specific task instance
     */
    @Column(name = "max_retries_override")
    private Integer maxRetriesOverride;

    /**
     * Tasks that must complete before this task can start (dependency keys)
     */
    @ElementCollection
    @CollectionTable(name = "workflow_task_dependencies", 
                     joinColumns = @JoinColumn(name = "workflow_task_id"))
    @Column(name = "depends_on_task_key")
    @Builder.Default
    private Set<String> dependsOn = new HashSet<>();

    @OneToMany(mappedBy = "workflowTask", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<TaskExecution> executions = new HashSet<>();

    /**
     * Get effective timeout (override or definition default)
     */
    public int getEffectiveTimeout() {
        return timeoutOverrideSeconds != null ? timeoutOverrideSeconds : taskDefinition.getTimeoutSeconds();
    }

    /**
     * Get effective max retries (override or definition default)
     */
    public int getEffectiveMaxRetries() {
        return maxRetriesOverride != null ? maxRetriesOverride : taskDefinition.getMaxRetries();
    }

    /**
     * Get the display name (override or task definition name)
     */
    public String getDisplayName() {
        return name != null ? name : taskDefinition.getName();
    }
}
