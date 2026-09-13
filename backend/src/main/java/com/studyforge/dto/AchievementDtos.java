/*
 * Decompiled with CFR 0.152.
 */
package com.studyforge.dto;

import java.time.Instant;
import java.util.List;

public class AchievementDtos {

    public record AchievementResponse(List<AchievementInfo> achievements, List<AchievementBrief> newlyUnlocked) {
    }

    public record AchievementInfo(String id, String name, String description, String icon, boolean unlocked, Instant unlockedAt) {
    }

    public record Unlocked(String id, Instant unlockedAt) {
    }

    public record AchievementBrief(String id, String name, String description, String icon) {
    }
}

