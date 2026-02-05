package com.flowforge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricsResponse {

    private long totalUsers;
    private long activeUsers;
    private long totalWorkflows;
    private long activeWorkflows;
    private long totalExecutions;
    private long completedExecutions;
    private long failedExecutions;
    private double successRate;
    private double avgExecutionDurationSeconds;
    private Map<String, Long> statusDistribution;
    private Instant lastUpdated;
}
