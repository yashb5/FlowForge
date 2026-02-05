package com.flowforge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowMetricsResponse {

    private UUID workflowId;
    private String workflowName;
    private long totalExecutions;
    private long successfulExecutions;
    private long failedExecutions;
    private double successRate;
    private double avgDurationSeconds;
    private long maxDurationSeconds;
    private long minDurationSeconds;
    private List<TaskMetrics> taskMetrics;
    private List<DailyTrend> dailyTrends;
    private Instant lastUpdated;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskMetrics {
        private String taskKey;
        private String taskName;
        private long executions;
        private long successes;
        private long failures;
        private double successRate;
        private double avgDurationSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrend {
        private String date;
        private long executions;
        private long successes;
        private double successRate;
    }
}
