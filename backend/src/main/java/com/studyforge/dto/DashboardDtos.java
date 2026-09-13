/*
 * Decompiled with CFR 0.152.
 */
package com.studyforge.dto;

import com.studyforge.dto.TargetDtos;
import java.util.List;

public class DashboardDtos {

    public record DashboardResponse(int totalTargets, int activeTargets, int completedTargets, int totalTasks, int doneTasks, double overallProgress, List<TodayTask> todayTasks, List<UpcomingDeadline> upcomingDeadlines, List<TargetDtos.TargetView> recentTargets, List<StatItem> quickStats) {
    }

    public record UpcomingDeadline(String targetId, String title, String deadline, int remainingDays) {
    }

    public record TodayTask(String id, String targetId, String targetTitle, String milestoneId, String title, String dueDate, String priority, String assigneeName) {
    }

    public record StatItem(String label, int value) {
    }
}

