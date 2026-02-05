package com.flowforge.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteWorkflowRequest {

    private Map<String, Object> input;

    @Size(max = 100, message = "Correlation ID must not exceed 100 characters")
    private String correlationId;

    @Builder.Default
    private Boolean async = true;
}
