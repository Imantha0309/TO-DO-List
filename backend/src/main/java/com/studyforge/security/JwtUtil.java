/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.jsonwebtoken.Claims
 *  io.jsonwebtoken.Jwts
 *  io.jsonwebtoken.security.Keys
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Component
 */
package com.studyforge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value(value="${studyforge.jwt.secret}") String secret, @Value(value="${studyforge.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor((byte[])secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(String userId, String email, String name) {
        Date now = new Date();
        return Jwts.builder().subject(userId).claim("email", (Object)email).claim("name", (Object)name).issuedAt(now).expiration(new Date(now.getTime() + this.expirationMs)).signWith((Key)this.key).compact();
    }

    public String extractUserId(String token) {
        return ((Claims)Jwts.parser().verifyWith(this.key).build().parseSignedClaims((CharSequence)token).getPayload()).getSubject();
    }

    public boolean isValid(String token) {
        try {
            this.extractUserId(token);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}

