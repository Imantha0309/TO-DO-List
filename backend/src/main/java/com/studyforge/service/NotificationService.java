package com.studyforge.service;

import com.studyforge.dto.NotificationsDtos;
import com.studyforge.exception.ApiException;
import com.studyforge.model.Target;
import com.studyforge.model.User;
import com.studyforge.repository.TargetRepository;
import com.studyforge.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final TargetRepository targetRepository;
    private final UserRepository userRepository;

    public NotificationService(TargetRepository targetRepository, UserRepository userRepository) {
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
    }

    public NotificationsDtos.NotificationsResponse unread(String userId) {
        User user = (User)this.userRepository.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account not found"));
        Instant seen = user.lastSeenNotificationsAt;
        ArrayList<NotificationsDtos.NotificationItem> items = new ArrayList<NotificationsDtos.NotificationItem>();
        for (Target t : this.targetRepository.findByCollaboratorIdsContaining(userId)) {
            if (t.activities == null) continue;
            boolean isOwner = t.ownerId.equals(userId);
            for (Target.Activity a : t.activities) {
                if (a.at == null || a.actorId == null || a.actorId.equals(userId)) continue;
                if (seen != null && !a.at.isAfter(seen)) continue;
                String type = a.type == null ? "edit" : a.type;
                if (!type.equals("edit") && !type.equals("share") && !type.equals("member") && !type.equals("ai")) continue;
                if (!isOwner && a.actorId.equals(t.ownerId) && type.equals("edit")) continue;
                items.add(new NotificationsDtos.NotificationItem(type, a.message, a.actorName, t.id, t.title, a.at));
            }
        }
        items.sort(Comparator.comparing(NotificationsDtos.NotificationItem::at).reversed());
        int size = items.size();
        return new NotificationsDtos.NotificationsResponse(size, items.subList(0, Math.min(size, 30)));
    }

    public void markRead(String userId) {
        User user = (User)this.userRepository.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account not found"));
        user.lastSeenNotificationsAt = Instant.now();
        this.userRepository.save(user);
    }
}