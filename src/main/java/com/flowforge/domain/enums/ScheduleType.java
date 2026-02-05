package com.flowforge.domain.enums;

/**
 * Types of schedules for workflow execution.
 */
public enum ScheduleType {
    /**
     * Cron expression-based schedule
     */
    CRON,

    /**
     * Fixed interval schedule
     */
    INTERVAL,

    /**
     * One-time execution at a specific time
     */
    ONE_TIME,

    /**
     * Event-driven (triggered by external events)
     */
    EVENT_DRIVEN
}
