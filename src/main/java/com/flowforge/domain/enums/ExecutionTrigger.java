package com.flowforge.domain.enums;

/**
 * How the workflow execution was triggered.
 */
public enum ExecutionTrigger {
    /**
     * Manually triggered via API
     */
    MANUAL,

    /**
     * Triggered by a schedule
     */
    SCHEDULED,

    /**
     * Triggered by an external webhook
     */
    WEBHOOK,

    /**
     * Triggered by a parent workflow (sub-workflow)
     */
    SUB_WORKFLOW,

    /**
     * Triggered by an event
     */
    EVENT,

    /**
     * Triggered by a retry mechanism
     */
    RETRY,

    /**
     * Triggered by API integration
     */
    API
}
