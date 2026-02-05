package com.flowforge.controller;

import com.flowforge.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health and observability endpoints for Kubernetes probes")
public class HealthController {

    private final HealthEndpoint healthEndpoint;
    private final LivenessStateHealthIndicator livenessIndicator;
    private final ReadinessStateHealthIndicator readinessIndicator;

    @GetMapping
    @Operation(summary = "Get overall health status")
    public ResponseEntity<ApiResponse<HealthComponent>> getHealth() {
        HealthComponent health = healthEndpoint.health();
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @GetMapping("/liveness")
    @Operation(summary = "Kubernetes liveness probe")
    public ResponseEntity<ApiResponse<HealthComponent>> liveness() {
        HealthComponent health = livenessIndicator.health();
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @GetMapping("/readiness")
    @Operation(summary = "Kubernetes readiness probe")
    public ResponseEntity<ApiResponse<HealthComponent>> readiness() {
        HealthComponent health = readinessIndicator.health();
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
