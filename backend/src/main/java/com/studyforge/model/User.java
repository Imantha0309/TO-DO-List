/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.annotation.Id
 *  org.springframework.data.mongodb.core.index.Indexed
 *  org.springframework.data.mongodb.core.mapping.Document
 */
package com.studyforge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(value="users")
public class User {
    @Id
    public String id;
    public String name;
    @Indexed(unique=true)
    public String email;
    public String passwordHash;
    public Instant createdAt = Instant.now();
    public List<UnlockedAchievement> unlocked = new ArrayList<UnlockedAchievement>();
    public Instant lastSeenNotificationsAt;

    public static class UnlockedAchievement {
        public String id;
        public Instant unlockedAt;

        public UnlockedAchievement() {
        }

        public UnlockedAchievement(String id, Instant unlockedAt) {
            this.id = id;
            this.unlockedAt = unlockedAt;
        }
    }
}

