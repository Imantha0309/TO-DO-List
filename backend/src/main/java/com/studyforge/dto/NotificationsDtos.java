package com.studyforge.dto;

import java.time.Instant;
import java.util.List;

public class NotificationsDtos {

    public record NotificationsResponse(int count, List<NotificationItem> items) {
    }

    public record NotificationItem(String type, String message, String actorName, String targetId, String targetTitle, Instant at) {
    }
}