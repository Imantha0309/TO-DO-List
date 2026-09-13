package com.studyforge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_REQUESTS = 240;
    private static final long WINDOW_MILLIS = 60000L;
    private final ConcurrentHashMap<String, ArrayDeque<Long>> hits = new ConcurrentHashMap<String, ArrayDeque<Long>>();

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String clientKey = this.clientKey(request);
        long now = System.currentTimeMillis();
        ArrayDeque<Long> deque = this.hits.computeIfAbsent(clientKey, k -> new ArrayDeque<Long>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MILLIS) {
                deque.removeFirst();
            }
            if (deque.size() >= MAX_REQUESTS) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"error\":\"Too many requests. Please slow down.\"}");
                return;
            }
            deque.addLast(now);
        }
        chain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}