package com.flowforge.config;

import com.flowforge.repository.UserRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final UserRepository userRepository;

    public DatabaseHealthIndicator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Health health() {
        try {
            long count = userRepository.count();
            return Health.up()
                    .withDetail("users_count", count)
                    .withDetail("status", "connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
