package com.flowforge.service;

import com.flowforge.domain.entity.User;
import com.flowforge.dto.request.LoginRequest;
import com.flowforge.dto.response.AuthResponse;
import com.flowforge.dto.response.UserResponse;
import com.flowforge.exception.AuthenticationException;
import com.flowforge.mapper.UserMapper;
import com.flowforge.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsernameOrEmail());

        User user;
        try {
            user = userService.findUserByUsernameOrEmail(request.getUsernameOrEmail());
        } catch (Exception e) {
            throw new AuthenticationException("Invalid credentials");
        }

        // Check if user is active
        if (!user.getIsActive()) {
            throw new AuthenticationException("Account is disabled");
        }

        // Check if user is locked
        if (user.isLocked()) {
            throw new AuthenticationException("Account is locked. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            userService.recordLoginFailure(user.getId());
            throw new AuthenticationException("Invalid credentials");
        }

        // Record successful login
        userService.recordLoginSuccess(user.getId());

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user);
        long expiresIn = jwtTokenProvider.getExpirationMs() / 1000;

        UserResponse userResponse = userMapper.toResponse(user);

        log.info("Login successful for user: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userResponse)
                .build();
    }

    public AuthResponse refreshToken(String token) {
        log.info("Refreshing token");

        if (!jwtTokenProvider.validateToken(token)) {
            throw new AuthenticationException("Invalid or expired token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        User user = userService.findUserByUsernameOrEmail(username);

        if (!user.getIsActive()) {
            throw new AuthenticationException("Account is disabled");
        }

        String newToken = jwtTokenProvider.generateToken(user);
        long expiresIn = jwtTokenProvider.getExpirationMs() / 1000;

        UserResponse userResponse = userMapper.toResponse(user);

        return AuthResponse.builder()
                .accessToken(newToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userResponse)
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userService.findUserByUsernameOrEmail(username);
        return userMapper.toResponse(user);
    }
}
