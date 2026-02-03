package com.flowforge.dto.request;

import com.flowforge.domain.enums.RetryStrategy;
import com.flowforge.domain.enums.TaskType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskDefinitionRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Task type is required")
    private TaskType taskType;

    @NotBlank(message = "Handler is required")
    @Size(max = 10000, message = "Handler must not exceed 10000 characters")
    private String handler;

    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)$", message = "Invalid HTTP method")
    private String httpMethod;

    private Map<String, String> defaultHeaders;

    private Map<String, Object> defaultInput;

    private String inputSchema;

    private String outputSchema;

    @Builder.Default
    private RetryStrategy retryStrategy = RetryStrategy.EXPONENTIAL_BACKOFF;

    @Min(value = 0, message = "Max retries cannot be negative")
    @Max(value = 100, message = "Max retries cannot exceed 100")
    @Builder.Default
    private Integer maxRetries = 3;

    @Min(value = 1, message = "Retry delay must be at least 1 second")
    @Max(value = 86400, message = "Retry delay cannot exceed 1 day")
    @Builder.Default
    private Integer retryDelaySeconds = 60;

    @DecimalMin(value = "1.0", message = "Retry multiplier must be at least 1.0")
    @DecimalMax(value = "10.0", message = "Retry multiplier cannot exceed 10.0")
    @Builder.Default
    private Double retryMultiplier = 2.0;

    @Min(value = 1, message = "Max retry delay must be at least 1 second")
    @Max(value = 86400, message = "Max retry delay cannot exceed 1 day")
    @Builder.Default
    private Integer maxRetryDelaySeconds = 3600;

    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Max(value = 86400, message = "Timeout cannot exceed 1 day")
    @Builder.Default
    private Integer timeoutSeconds = 300;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority cannot exceed 10")
    @Builder.Default
    private Integer priority = 5;

    private Set<String> tags;
}
