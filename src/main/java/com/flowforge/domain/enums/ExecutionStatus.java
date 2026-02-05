package com.flowforge.domain.enums;

/**
 * Status of a workflow or task execution.
 */
public enum ExecutionStatus {
    /**
     * Execution is queued and waiting to start
     */
    PENDING,

    /**
     * Execution is queued and scheduled to run
     */
    SCHEDULED,

    /**
     * Execution is currently running
     */
    RUNNING,

    /**
     * Execution completed successfully
     */
    COMPLETED,

    /**
     * Execution failed
     */
    FAILED,

    /**
     * Execution was cancelled by user
     */
    CANCELLED,

    /**
     * Execution timed out
     */
    TIMED_OUT,

    /**
     * Task was skipped due to condition evaluation
     */
    SKIPPED,

    /**
     * Waiting for retry
     */
    WAITING_FOR_RETRY,

    /**
     * Execution is paused
     */
    PAUSED,

    /**
     * Blocked waiting for dependencies
     */
    BLOCKED
}
