package com.flowforge.dto.request;

import com.flowforge.domain.enums.ScheduleType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduleRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;

    @Size(max = 100, message = "Cron expression must not exceed 100 characters")
    private String cronExpression;

    @Min(value = 60, message = "Interval must be at least 60 seconds")
    @Max(value = 31536000, message = "Interval cannot exceed 1 year")
    private Long intervalSeconds;

    @Future(message = "Run at time must be in the future")
    private Instant runAt;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    @Builder.Default
    private String timezone = "UTC";

    private Instant startAt;

    @Future(message = "End time must be in the future")
    private Instant endAt;

    private Map<String, Object> inputOverrides;

    @Min(value = 1, message = "Max executions must be at least 1")
    private Integer maxExecutions;

    @Min(value = 1, message = "Max concurrent executions must be at least 1")
    @Max(value = 10, message = "Max concurrent executions cannot exceed 10")
    @Builder.Default
    private Integer maxConcurrentExecutions = 1;

    @Builder.Default
    private Boolean catchUpMissed = false;

    private Set<String> tags;
}
