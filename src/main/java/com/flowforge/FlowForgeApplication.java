package com.flowforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FlowForge - Distributed Task Scheduling and Workflow Engine
 * 
 * A comprehensive workflow management system that supports:
 * - DAG-based workflow definitions with task dependencies
 * - Cron-based and one-time task scheduling
 * - State machine-driven task execution
 * - Retry policies with exponential backoff
 * - Complete audit trail and execution history
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableCaching
public class FlowForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowForgeApplication.class, args);
    }
}
