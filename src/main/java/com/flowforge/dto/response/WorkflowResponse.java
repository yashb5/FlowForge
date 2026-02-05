package com.flowforge.dto.response;

import com.flowforge.domain.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private String ownerUsername;
    private WorkflowStatus status;
    private Map<String, Object> globalInput;
    private Map<String, String> outputMapping;
    private Integer maxConcurrency;
    private Integer timeoutSeconds;
    private Boolean continueOnFailure;
    private String errorHandler;
    private String completionWebhookUrl;
    private Boolean isActive;
    private Set<String> tags;
    private List<WorkflowTaskResponse> tasks;
    private Integer taskCount;
    private Integer scheduleCount;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowTaskResponse {
        private UUID id;
        private String taskKey;
        private String name;
        private UUID taskDefinitionId;
        private String taskDefinitionName;
        private Integer executionOrder;
        private Map<String, Object> inputOverrides;
        private Map<String, String> inputMapping;
        private String conditionExpression;
        private Boolean isEnabled;
        private Integer timeoutOverrideSeconds;
        private Integer maxRetriesOverride;
        private Set<String> dependsOn;
    }
}
