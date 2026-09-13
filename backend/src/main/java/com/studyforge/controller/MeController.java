package com.studyforge.controller;

import com.studyforge.dto.NotificationsDtos;
import com.studyforge.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/me"})
public class MeController {
    private final NotificationService notificationService;

    public MeController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping(value={"/notifications"})
    public NotificationsDtos.NotificationsResponse notifications(Authentication authentication) {
        return this.notificationService.unread(authentication.getName());
    }

    @PostMapping(value={"/notifications/read"})
    public void markNotificationsRead(Authentication authentication) {
        this.notificationService.markRead(authentication.getName());
    }
}