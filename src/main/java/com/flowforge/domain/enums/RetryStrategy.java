package com.flowforge.domain.enums;

/**
 * Strategies for retrying failed tasks.
 */
public enum RetryStrategy {
    /**
     * No retries - fail immediately
     */
    NONE,

    /**
     * Fixed delay between retries
     */
    FIXED,

    /**
     * Exponential backoff - delay doubles after each retry
     */
    EXPONENTIAL_BACKOFF,

    /**
     * Linear backoff - delay increases linearly
     */
    LINEAR_BACKOFF,

    /**
     * Random jitter added to fixed delay
     */
    JITTER
}
