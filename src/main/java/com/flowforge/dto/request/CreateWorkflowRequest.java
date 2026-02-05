package com.flowforge.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private Map<String, Object> globalInput;

    private Map<String, String> outputMapping;

    @Min(value = 1, message = "Max concurrency must be at least 1")
    @Max(value = 100, message = "Max concurrency cannot exceed 100")
    @Builder.Default
    private Integer maxConcurrency = 10;

    @Min(value = 60, message = "Timeout must be at least 60 seconds")
    @Max(value = 86400, message = "Timeout cannot exceed 1 day")
    @Builder.Default
    private Integer timeoutSeconds = 3600;

    @Builder.Default
    private Boolean continueOnFailure = false;

    @Size(max = 10000, message = "Error handler must not exceed 10000 characters")
    private String errorHandler;

    @Size(max = 500, message = "Webhook URL must not exceed 500 characters")
    @Pattern(regexp = "^(https?://.*)?$", message = "Invalid webhook URL format")
    private String completionWebhookUrl;

    private Set<String> tags;

    @Valid
    private List<WorkflowTaskRequest> tasks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowTaskRequest {

        @NotNull(message = "Task definition ID is required")
        private java.util.UUID taskDefinitionId;

        @NotBlank(message = "Task key is required")
        @Size(max = 100, message = "Task key must not exceed 100 characters")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]*$", 
                 message = "Task key must start with a letter and contain only letters, numbers, underscores, and hyphens")
        private String taskKey;

        @Size(max = 255, message = "Name must not exceed 255 characters")
        private String name;

        @Min(value = 0, message = "Execution order cannot be negative")
        @Builder.Default
        private Integer executionOrder = 0;

        private Map<String, Object> inputOverrides;

        private Map<String, String> inputMapping;

        private String conditionExpression;

        @Builder.Default
        private Boolean isEnabled = true;

        private Integer timeoutOverrideSeconds;

        private Integer maxRetriesOverride;

        private Set<String> dependsOn;
    }
}
