package com.flowforge.dto.response;

import com.flowforge.domain.enums.RetryStrategy;
import com.flowforge.domain.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDefinitionResponse {

    private UUID id;
    private String name;
    private String description;
    private TaskType taskType;
    private UUID ownerId;
    private String ownerUsername;
    private String handler;
    private String httpMethod;
    private Map<String, String> defaultHeaders;
    private Map<String, Object> defaultInput;
    private String inputSchema;
    private String outputSchema;
    private RetryStrategy retryStrategy;
    private Integer maxRetries;
    private Integer retryDelaySeconds;
    private Double retryMultiplier;
    private Integer maxRetryDelaySeconds;
    private Integer timeoutSeconds;
    private Integer priority;
    private Boolean isActive;
    private Set<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
