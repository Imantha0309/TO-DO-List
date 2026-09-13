/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.constraints.Email
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.Size
 */
package com.studyforge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record AuthResponse(String token, UserView user) {
    }

    public record UserView(String id, String name, String email) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record RegisterRequest(@NotBlank @Size(min=2, max=60) @NotBlank @Size(min=2, max=60) String name, @NotBlank @Email String email, @NotBlank @Size(min=6, max=72) @NotBlank @Size(min=6, max=72) String password) {
    }

    public record UpdateProfileRequest(@NotBlank @Size(min=2, max=60) String name) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min=6, max=72) String newPassword) {
    }
}

