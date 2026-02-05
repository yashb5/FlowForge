package com.flowforge.domain.enums;

/**
 * User roles for authorization.
 */
public enum UserRole {
    /**
     * Regular user - can manage their own workflows and tasks
     */
    USER,

    /**
     * Power user - can view and execute workflows owned by others
     */
    OPERATOR,

    /**
     * Administrator - full system access
     */
    ADMIN
}
