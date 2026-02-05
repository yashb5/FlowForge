package com.flowforge.dto.response;

import com.flowforge.domain.enums.ExecutionStatus;
import com.flowforge.domain.enums.ExecutionTrigger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionResponse {

    private UUID id;
    private UUID workflowId;
    private String workflowName;
    private UUID scheduleId;
    private String scheduleName;
    private UUID triggeredById;
    private String triggeredByUsername;
    private ExecutionStatus status;
    private ExecutionTrigger triggerType;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private Instant startedAt;
    private Instant completedAt;
    private Instant timeoutAt;
    private Long durationMs;
    private String errorMessage;
    private Integer tasksCompleted;
    private Integer tasksFailed;
    private Integer tasksTotal;
    private Double progressPercentage;
    private Boolean isCancelled;
    private String cancellationReason;
    private String correlationId;
    private List<TaskExecutionResponse> taskExecutions;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskExecutionResponse {
        private UUID id;
        private String taskKey;
        private String taskName;
        private ExecutionStatus status;
        private Integer attemptNumber;
        private Integer maxAttempts;
        private Map<String, Object> input;
        private Map<String, Object> output;
        private Instant queuedAt;
        private Instant startedAt;
        private Instant completedAt;
        private Long durationMs;
        private String errorMessage;
        private String errorCode;
        private Integer httpStatusCode;
        private Boolean isSkipped;
        private String workerId;
    }
}
