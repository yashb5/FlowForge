package com.flowforge.service;

import com.flowforge.domain.entity.User;
import com.flowforge.domain.enums.UserRole;
import com.flowforge.dto.request.CreateUserRequest;
import com.flowforge.dto.request.UpdateUserRequest;
import com.flowforge.dto.response.UserResponse;
import com.flowforge.exception.DuplicateResourceException;
import com.flowforge.exception.ResourceNotFoundException;
import com.flowforge.mapper.UserMapper;
import com.flowforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user with username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        if (request.getRole() == null) {
            user.setRole(UserRole.USER);
        }

        User savedUser = userRepository.save(user);
        log.info("Created user with ID: {}", savedUser.getId());
        
        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = findUserById(id);
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findByIsActiveTrue(pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable)
                .map(userMapper::toResponse);
    }

    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);
        
        User user = findUserById(id);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        User updatedUser = userRepository.save(user);
        log.info("Updated user with ID: {}", updatedUser.getId());
        
        return userMapper.toResponse(updatedUser);
    }

    public void deleteUser(UUID id) {
        log.info("Deleting user with ID: {}", id);
        
        User user = findUserById(id);
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("Soft deleted user with ID: {}", id);
    }

    public void recordLoginSuccess(UUID userId) {
        userRepository.updateLoginSuccess(userId, Instant.now());
        log.info("Recorded successful login for user: {}", userId);
    }

    public void recordLoginFailure(UUID userId) {
        userRepository.incrementFailedLoginAttempts(userId);
        
        User user = findUserById(userId);
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            Instant lockedUntil = Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60);
            userRepository.lockUser(userId, lockedUntil);
            log.warn("User {} has been locked until {} due to too many failed login attempts", 
                    userId, lockedUntil);
        }
    }

    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public User findUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username/email", usernameOrEmail));
    }
}
