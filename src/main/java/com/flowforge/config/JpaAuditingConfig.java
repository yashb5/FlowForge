package com.flowforge.config;

import com.flowforge.security.CustomUserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserPrincipal) {
                return Optional.of(((CustomUserPrincipal) principal).getUsername());
            } else if (principal instanceof String) {
                return Optional.of((String) principal);
            }

            return Optional.of("anonymous");
        };
    }
}
