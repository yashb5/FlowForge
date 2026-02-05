package com.flowforge.dto.response;

import com.flowforge.domain.enums.Granularity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTrendResponse {

    private Granularity granularity;
    private List<TrendPoint> trends;
    private Instant lastUpdated;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private Instant timestamp;
        private long totalExecutions;
        private Map<String, Long> statusCounts;
        private double successRate;
    }
}
