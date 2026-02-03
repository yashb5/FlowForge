package com.flowforge.domain.enums;

/**
 * Status of a workflow definition.
 */
public enum WorkflowStatus {
    /**
     * Workflow is being designed, not ready for execution
     */
    DRAFT,

    /**
     * Workflow is active and can be executed
     */
    ACTIVE,

    /**
     * Workflow is temporarily disabled
     */
    PAUSED,

    /**
     * Workflow is archived and cannot be executed
     */
    ARCHIVED,

    /**
     * Workflow is being validated
     */
    VALIDATING,

    /**
     * Workflow has validation errors
     */
    INVALID
}
