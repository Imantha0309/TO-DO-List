/*
 * Decompiled with CFR 0.152.
 */
package com.studyforge.dto;

import com.studyforge.dto.AchievementDtos;
import com.studyforge.model.Member;
import com.studyforge.model.Milestone;
import com.studyforge.model.Priority;
import com.studyforge.model.Target;
import com.studyforge.model.TargetSource;
import com.studyforge.model.TargetStatus;
import com.studyforge.model.Task;
import com.studyforge.model.TaskStatus;
import java.time.LocalDate;
import java.util.List;

public class TargetDtos {
    public static TaskItem toTaskItem(Task t) {
        if (t == null) {
            return null;
        }
        return new TaskItem(t.id, t.title, t.description, t.dueDate, t.priority, t.status, t.assigneeName, t.dependsOn, t.subtasks == null ? List.of() : t.subtasks.stream().map(s -> new SubtaskItem(s.id, s.body, s.done)).toList());
    }

    public static MilestoneItem toMilestoneItem(Milestone m) {
        if (m == null) {
            return null;
        }
        return new MilestoneItem(m.id, m.title, m.endDate, m.notes, m.tasks == null ? List.of() : m.tasks.stream().map(TargetDtos::toTaskItem).toList());
    }

    public record TaskItem(String id, String title, String description, LocalDate dueDate, Priority priority, TaskStatus status, String assigneeName, List<String> dependsOn, List<SubtaskItem> subtasks) {
    }

    public record MilestoneItem(String id, String title, LocalDate endDate, String notes, List<TaskItem> tasks) {
    }

    public record SubtaskItem(String id, String body, boolean done) {
    }

    public record TargetResponse(TargetView target, List<AchievementDtos.AchievementBrief> newlyUnlocked) {
    }

    public record TargetView(String id, String ownerId, String ownerName, String access, String title, String description, String courseModule, LocalDate startDate, LocalDate deadline, Priority priority, TargetStatus status, TargetSource source, String aiNotes, List<Member> members, List<MilestoneItem> milestones, ProgressStats stats, String createdAt, String updatedAt, List<Target.Activity> activities) {
    }

    public record CollaboratorRoleRequest(String role) {
    }

    public record ProgressStats(int taskTotal, int taskDone, int subtaskTotal, int subtaskDone, int milestoneTotal, int milestoneDone, int progress) {
    }

    public record CollaboratorRequest(String email) {
    }

    public record TargetRequest(String title, String description, String courseModule, LocalDate startDate, LocalDate deadline, Priority priority, TargetSource source, String aiNotes, List<MemberRequest> members, List<MilestoneRequest> milestones) {
    }

    public record MemberRequest(String name, String email, String role, String userId) {
    }

    public record MilestoneRequest(String title, LocalDate endDate, String notes, List<TaskRequest> tasks) {
    }

    public record TaskRequest(String title, String description, LocalDate dueDate, Priority priority, TaskStatus status, String assigneeName, List<String> dependsOn, List<SubtaskRequest> subtasks) {
    }

    public record SubtaskRequest(String body, Boolean done) {
    }
}

