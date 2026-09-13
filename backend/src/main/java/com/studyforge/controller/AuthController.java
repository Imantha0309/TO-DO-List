/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.Valid
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.studyforge.controller;

import com.studyforge.dto.AuthDtos;
import com.studyforge.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/auth"})
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value={"/register"})
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return this.authService.register(request);
    }

    @PostMapping(value={"/login"})
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return this.authService.login(request);
    }

    @GetMapping(value={"/me"})
    public AuthDtos.UserView me(Authentication authentication) {
        return this.authService.me(authentication.getName());
    }

    @PutMapping(value={"/me"})
    public AuthDtos.UserView updateProfile(Authentication authentication, @Valid @RequestBody AuthDtos.UpdateProfileRequest request) {
        return this.authService.updateProfile(authentication.getName(), request);
    }

    @PutMapping(value={"/me/password"})
    public AuthDtos.UserView changePassword(Authentication authentication, @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        return this.authService.changePassword(authentication.getName(), request);
    }
}

