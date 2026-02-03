package com.flowforge.domain.entity;

import com.flowforge.domain.enums.WorkflowStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

/**
 * Workflow represents a DAG (Directed Acyclic Graph) of tasks that need to be executed.
 * It defines the execution order and dependencies between tasks.
 */
@Entity
@Table(name = "workflows", indexes = {
    @Index(name = "idx_workflow_name", columnList = "name"),
    @Index(name = "idx_workflow_owner", columnList = "owner_id"),
    @Index(name = "idx_workflow_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    /**
     * Global input parameters that are available to all tasks in the workflow
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "global_input", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> globalInput = new HashMap<>();

    /**
     * Output mapping - defines how task outputs map to workflow output
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_mapping", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> outputMapping = new HashMap<>();

    /**
     * Maximum concurrent tasks that can run at the same time
     */
    @Column(name = "max_concurrency", nullable = false)
    @Builder.Default
    private Integer maxConcurrency = 10;

    /**
     * Overall timeout for the entire workflow in seconds
     */
    @Column(name = "timeout_seconds", nullable = false)
    @Builder.Default
    private Integer timeoutSeconds = 3600;

    /**
     * Whether to continue executing remaining tasks if one fails
     */
    @Column(name = "continue_on_failure", nullable = false)
    @Builder.Default
    private Boolean continueOnFailure = false;

    /**
     * Error handling strategy for the workflow
     */
    @Column(name = "error_handler", columnDefinition = "TEXT")
    private String errorHandler;

    /**
     * Webhook URL to notify when workflow completes
     */
    @Column(name = "completion_webhook_url", length = 500)
    private String completionWebhookUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Tags for categorization and filtering
     */
    @ElementCollection
    @CollectionTable(name = "workflow_tags", joinColumns = @JoinColumn(name = "workflow_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("executionOrder ASC")
    @Builder.Default
    private List<WorkflowTask> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Schedule> schedules = new HashSet<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<WorkflowExecution> executions = new HashSet<>();

    public void addTask(WorkflowTask task) {
        tasks.add(task);
        task.setWorkflow(this);
    }

    public void removeTask(WorkflowTask task) {
        tasks.remove(task);
        task.setWorkflow(null);
    }
}
