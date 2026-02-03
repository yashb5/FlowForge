package com.flowforge.mapper;

import com.flowforge.domain.entity.User;
import com.flowforge.dto.request.CreateUserRequest;
import com.flowforge.dto.response.UserResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "failedLoginAttempts", constant = "0")
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "workflows", ignore = true)
    @Mapping(target = "taskDefinitions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    User toEntity(CreateUserRequest request);

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    UserResponse toResponse(User user);
}
