package com.flowforge.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for security-related operations used in SpEL expressions.
 */
@Service
public class SecurityService {

    /**
     * Check if the current authenticated user matches the given user ID.
     */
    public boolean isCurrentUser(UUID userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserPrincipal) {
            return ((CustomUserPrincipal) principal).getUserId().equals(userId);
        }

        return false;
    }

    /**
     * Get the current authenticated user's ID.
     */
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserPrincipal) {
            return ((CustomUserPrincipal) principal).getUserId();
        }

        return null;
    }
}
