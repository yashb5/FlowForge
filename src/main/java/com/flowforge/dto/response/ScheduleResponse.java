package com.flowforge.dto.response;

import com.flowforge.domain.enums.ScheduleType;
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
public class ScheduleResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID workflowId;
    private String workflowName;
    private ScheduleType scheduleType;
    private String cronExpression;
    private Long intervalSeconds;
    private Instant runAt;
    private String timezone;
    private Instant startAt;
    private Instant endAt;
    private Instant nextRunAt;
    private Instant lastRunAt;
    private Map<String, Object> inputOverrides;
    private Integer maxExecutions;
    private Integer executionCount;
    private Integer maxConcurrentExecutions;
    private Boolean catchUpMissed;
    private Boolean isActive;
    private Boolean isPaused;
    private Set<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
