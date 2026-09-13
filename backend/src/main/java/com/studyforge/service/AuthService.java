/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.security.crypto.password.PasswordEncoder
 *  org.springframework.stereotype.Service
 */
package com.studyforge.service;

import com.studyforge.dto.AuthDtos;
import com.studyforge.exception.ApiException;
import com.studyforge.model.User;
import com.studyforge.repository.UserRepository;
import com.studyforge.security.JwtUtil;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (this.userRepository.findByEmail(request.email().toLowerCase().trim()).isPresent()) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = new User();
        user.name = request.name().trim();
        user.email = request.email().toLowerCase().trim();
        user.passwordHash = this.passwordEncoder.encode((CharSequence)request.password());
        user = (User)this.userRepository.save(user);
        return this.toResponse(user);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = this.userRepository.findByEmail(request.email().toLowerCase().trim()).orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!this.passwordEncoder.matches((CharSequence)request.password(), user.passwordHash)) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return this.toResponse(user);
    }

    public AuthDtos.UserView me(String userId) {
        User user = (User)this.userRepository.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account not found"));
        return new AuthDtos.UserView(user.id, user.name, user.email);
    }

    public AuthDtos.UserView updateProfile(String userId, AuthDtos.UpdateProfileRequest request) {
        User user = (User)this.userRepository.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account not found"));
        user.name = request.name().trim();
        this.userRepository.save(user);
        return new AuthDtos.UserView(user.id, user.name, user.email);
    }

    public AuthDtos.UserView changePassword(String userId, AuthDtos.ChangePasswordRequest request) {
        User user = (User)this.userRepository.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account not found"));
        if (!this.passwordEncoder.matches((CharSequence)request.currentPassword(), user.passwordHash)) {
            throw ApiException.unauthorized("Current password is incorrect");
        }
        user.passwordHash = this.passwordEncoder.encode((CharSequence)request.newPassword());
        user.lastSeenNotificationsAt = Instant.now();
        this.userRepository.save(user);
        return new AuthDtos.UserView(user.id, user.name, user.email);
    }

    private AuthDtos.AuthResponse toResponse(User user) {
        String token = this.jwtUtil.generate(user.id, user.email, user.name);
        return new AuthDtos.AuthResponse(token, new AuthDtos.UserView(user.id, user.name, user.email));
    }
}

