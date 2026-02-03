package com.flowforge.domain.enums;

/**
 * Types of tasks that can be executed.
 */
public enum TaskType {
    /**
     * HTTP/REST API call
     */
    HTTP,

    /**
     * Shell script execution
     */
    SCRIPT,

    /**
     * Message queue publish
     */
    QUEUE,

    /**
     * Database query/operation
     */
    DATABASE,

    /**
     * File operation (read/write/transform)
     */
    FILE,

    /**
     * Email sending
     */
    EMAIL,

    /**
     * Webhook notification
     */
    WEBHOOK,

    /**
     * Wait/delay task
     */
    DELAY,

    /**
     * Sub-workflow execution
     */
    SUB_WORKFLOW,

    /**
     * Custom/plugin task type
     */
    CUSTOM
}
