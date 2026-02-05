package com.flowforge.dto.response;

import com.flowforge.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private UserRole role;
    private Boolean isActive;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}
