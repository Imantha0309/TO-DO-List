/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.studyforge.service;

import com.studyforge.dto.TargetDtos;
import com.studyforge.exception.ApiException;
import com.studyforge.model.Member;
import com.studyforge.model.Milestone;
import com.studyforge.model.Priority;
import com.studyforge.model.Subtask;
import com.studyforge.model.Target;
import com.studyforge.model.TargetSource;
import com.studyforge.model.TargetStatus;
import com.studyforge.model.Task;
import com.studyforge.model.TaskStatus;
import com.studyforge.model.User;
import com.studyforge.repository.TargetRepository;
import com.studyforge.repository.UserRepository;
import com.studyforge.service.IdGen;
import com.studyforge.service.ProgressService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TargetService {
    public static final String ACCESS_OWNER = "OWNER";
    public static final String ACCESS_EDITOR = "EDITOR";
    public static final String ACCESS_VIEWER = "VIEWER";
    private final TargetRepository targetRepository;
    private final ProgressService progressService;
    private final UserRepository userRepository;

    public TargetService(TargetRepository targetRepository, ProgressService progressService, UserRepository userRepository) {
        this.targetRepository = targetRepository;
        this.progressService = progressService;
        this.userRepository = userRepository;
    }

    public List<TargetDtos.TargetView> list(String userId) {
        return this.accessibleTargets(userId).stream().sorted((a, b) -> b.updatedAt.compareTo(a.updatedAt)).map(t -> this.toView((Target)t, userId)).toList();
    }

    public TargetDtos.TargetView get(String userId, String targetId) {
        return this.toView(this.requireAccess(userId, targetId), userId);
    }

    public TargetDtos.TargetView create(String userId, TargetDtos.TargetRequest request) {
        Target target = new Target(userId, this.trim(request.title()));
        this.applyRequest(target, request);
        this.assignIds(target);
        target.source = request.source() == null ? TargetSource.MANUAL : request.source();
        target.status = TargetStatus.ACTIVE;
        target.completedAt = null;
        target.createdAt = Instant.now();
        this.logActivity(target, userId, "target", "Created this target");
        this.recomputeStatus(target);
        this.targetRepository.save(target);
        return this.toView(target, userId);
    }

    public TargetDtos.TargetView update(String userId, String targetId, TargetDtos.TargetRequest request) {
        Target target = this.requireAccess(userId, targetId);
        this.requireEditable(userId, target);
        if (target.status == TargetStatus.ARCHIVED) {
            throw ApiException.badRequest("Archived targets cannot be edited");
        }
        List<Milestone> previousMilestones = target.milestones == null ? new ArrayList<>() : new ArrayList<>(target.milestones);
        target.title = this.trim(request.title());
        this.applyRequest(target, request);
        this.restoreIds(target, previousMilestones);
        this.assignIds(target);
        target.updatedAt = Instant.now();
        this.logActivity(target, userId, "edit", "Edited the plan");
        this.recomputeStatus(target);
        this.targetRepository.save(target);
        return this.toView(target, userId);
    }

    public void appendActivity(String userId, String targetId, String message) {
        Target target = this.requireAccess(userId, targetId);
        this.logActivity(target, userId, "ai", message);
        target.updatedAt = Instant.now();
        this.targetRepository.save(target);
    }

    public void delete(String userId, String targetId) {
        Target target = this.requireOwner(userId, targetId);
        this.targetRepository.delete(target);
    }

    public TargetDtos.TargetView addMember(String userId, String targetId, TargetDtos.MemberRequest request) {
        Target target = this.requireOwner(userId, targetId);
        if (request.name() == null || request.name().isBlank()) {
            throw ApiException.badRequest("Member name is required");
        }
        Member member = new Member(IdGen.uid(), request.name().trim());
        member.email = request.email();
        member.role = request.role();
        member.userId = request.userId();
        this.linkMemberIfEmailMatches(member);
        target.members.add(member);
        target.updatedAt = Instant.now();
        this.logActivity(target, userId, "member", "Added member " + member.name);
        this.targetRepository.save(target);
        return this.toView(target, userId);
    }

    public TargetDtos.TargetView removeMember(String userId, String targetId, String memberId) {
        Target target = this.requireOwner(userId, targetId);
        Member removed = target.members.stream().filter(m -> m.id.equals(memberId)).findFirst().orElse(null);
        target.members.removeIf(m -> m.id.equals(memberId));
        this.unassignByMember(target, removed);
        target.updatedAt = Instant.now();
        if (removed != null) {
            this.logActivity(target, userId, "member", "Removed member " + removed.name);
        }
        this.targetRepository.save(target);
        return this.toView(target, userId);
    }

    public TargetDtos.TargetView addCollaborator(String userId, String targetId, String email) {
        String normalized;
        Target target = this.requireOwner(userId, targetId);
        String string = normalized = email == null ? "" : email.toLowerCase().trim();
        if (normalized.isEmpty()) {
            throw ApiException.badRequest("Email is required");
        }
        User user = this.userRepository.findByEmail(normalized).orElseThrow(() -> ApiException.notFound("No StudyForge account with that email"));
        if (user.id.equals(userId)) {
            throw ApiException.badRequest("That's you \u2014 a target is always shared with its owner");
        }
        if (!target.collaboratorIds.contains(user.id)) {
            target.collaboratorIds.add(user.id);
        }
        this.upsertLinkedMember(target, user);
        target.updatedAt = Instant.now();
        this.logActivity(target, userId, "share", "Invited " + user.email + " to collaborate");
        this.targetRepository.save(target);
        return this.toView(target, userId);
    }

    public TargetDtos.TargetView removeCollaborator(String actorId, String targetId, String collaboratorUserId) {
        Target target = this.requireAccess(actorId, targetId);
        boolean actorIsOwner = target.ownerId.equals(actorId);
        boolean removingSelf = actorId.equals(collaboratorUserId);
        if (!actorIsOwner && !removingSelf) {
            throw ApiException.forbidden("Only the owner can remove other collaborators");
        }
        target.collaboratorIds.remove(collaboratorUserId);
        Member removed = null;
        for (Member m2 : target.members) {
            if (!collaboratorUserId.equals(m2.userId)) continue;
            removed = m2;
            break;
        }
        if (removed != null) {
            target.members.removeIf(m -> collaboratorUserId.equals(m.userId));
        }
        this.unassignByMember(target, removed);
        target.updatedAt = Instant.now();
        this.logActivity(target, actorId, "share", actorIsOwner && !removingSelf ? "Removed a collaborator" : "Left the target");
        this.targetRepository.save(target);
        return this.toView(target, actorId);
    }

    public TargetDtos.TargetView setCollaboratorRole(String userId, String targetId, String collaboratorUserId, String role) {
        Target target = this.requireOwner(userId, targetId);
        String normalized = role == null ? "" : role.trim().toLowerCase();
        if (!normalized.equals("viewer") && !normalized.equals("editor")) {
            throw ApiException.badRequest("Role must be either 'viewer' or 'editor'");
        }
        boolean updated = false;
        for (Member m : target.members) {
            if (!collaboratorUserId.equals(m.userId)) continue;
            m.role = normalized;
            updated = true;
        }
        if (!updated) {
            throw ApiException.notFound("Collaborator not found on this target");
        }
        target.updatedAt = Instant.now();
        this.logActivity(target, userId, "share", normalized.equals("viewer") ? "Made a collaborator a viewer" : "Made a collaborator an editor");
        this.targetRepository.save(target);
        return this.toView(target, userId);
    }

    private void upsertLinkedMember(Target target, User user) {
        for (Member m : target.members) {
            if (user.id.equals(m.userId)) {
                m.name = user.name;
                m.email = user.email;
                return;
            }
            if (m.email == null || !m.email.equalsIgnoreCase(user.email)) continue;
            m.userId = user.id;
            m.name = user.name;
            m.role = m.role == null ? "member" : m.role;
            return;
        }
        Member member = new Member(IdGen.uid(), user.name);
        member.email = user.email;
        member.userId = user.id;
        member.role = "member";
        target.members.add(member);
    }

    private void linkMemberIfEmailMatches(Member member) {
        if (member.userId != null || member.email == null || member.email.isBlank()) {
            return;
        }
        this.userRepository.findByEmail(member.email.toLowerCase().trim()).ifPresent(u -> {
            member.userId = u.id;
        });
    }

    private void unassignByMember(Target target, Member removed) {
        if (removed == null || removed.name == null) {
            return;
        }
        for (Milestone m : target.milestones) {
            for (Task t : m.tasks) {
                if (t.assigneeName == null || !t.assigneeName.equalsIgnoreCase(removed.name.trim())) continue;
                t.assigneeName = null;
            }
        }
    }

    private void recomputeStatus(Target target) {
        if (this.progressService.isCompleted(target)) {
            target.status = TargetStatus.COMPLETED;
            if (target.completedAt == null) {
                target.completedAt = Instant.now();
            }
        } else if (target.status == TargetStatus.COMPLETED) {
            target.status = TargetStatus.ACTIVE;
        }
        if (target.status != TargetStatus.COMPLETED) {
            target.completedAt = null;
        }
    }

    private void applyRequest(Target target, TargetDtos.TargetRequest request) {
        target.description = this.trim(request.description());
        target.courseModule = this.trim(request.courseModule());
        target.startDate = request.startDate();
        target.deadline = request.deadline();
        target.priority = request.priority() == null ? Priority.MEDIUM : request.priority();
        target.aiNotes = this.trim(request.aiNotes());
        if (request.deadline() != null && request.startDate() != null && request.deadline().isBefore(request.startDate())) {
            throw ApiException.badRequest("Deadline cannot be before the start date");
        }
        ArrayList<Milestone> milestones = new ArrayList<Milestone>();
        if (request.milestones() != null) {
            for (TargetDtos.MilestoneRequest mr : request.milestones()) {
                Milestone milestone = new Milestone();
                milestone.id = null;
                milestone.title = this.trim(mr.title());
                milestone.endDate = mr.endDate();
                milestone.notes = this.trim(mr.notes());
                milestone.tasks = new ArrayList<Task>();
                if (mr.tasks() != null) {
                    for (TargetDtos.TaskRequest tr : mr.tasks()) {
                        Task task = this.toTask(tr);
                        task.id = null;
                        milestone.tasks.add(task);
                    }
                }
                milestones.add(milestone);
            }
        }
        target.milestones = milestones;
        ArrayList<Member> members = new ArrayList<Member>();
        if (request.members() != null) {
            for (TargetDtos.MemberRequest mbr : request.members()) {
                if (mbr.name() == null || mbr.name().isBlank()) continue;
                Member member = new Member(null, mbr.name().trim());
                member.email = mbr.email();
                member.role = mbr.role();
                member.userId = mbr.userId();
                members.add(member);
            }
        }
        target.members = members;
    }

    private Task toTask(TargetDtos.TaskRequest tr) {
        Task task = new Task();
        task.title = this.trim(tr.title());
        task.description = this.trim(tr.description());
        task.dueDate = tr.dueDate();
        task.priority = tr.priority() == null ? Priority.MEDIUM : tr.priority();
        task.status = tr.status() == null ? TaskStatus.TODO : tr.status();
        task.assigneeName = tr.assigneeName();
        task.subtasks = new ArrayList<Subtask>();
        if (tr.subtasks() != null) {
            for (TargetDtos.SubtaskRequest sr : tr.subtasks()) {
                if (sr.body() == null || sr.body().isBlank()) continue;
                Subtask s = new Subtask();
                if (sr.done() != null && sr.done().booleanValue()) {
                    s.done = true;
                }
                s.body = this.trim(sr.body());
                task.subtasks.add(s);
            }
        }
        return task;
    }

    private void restoreIds(Target target, List<Milestone> previousMilestones) {
        if (target.milestones == null || previousMilestones.isEmpty()) {
            return;
        }
        int count = Math.min(target.milestones.size(), previousMilestones.size());
        for (int i = 0; i < count; ++i) {
            Milestone current = target.milestones.get(i);
            current.id = previousMilestones.get(i).id;
            List<Task> previousTasks = previousMilestones.get(i).tasks;
            List<Task> currentTasks = current.tasks;
            if (currentTasks == null || previousTasks == null) continue;
            int k = 0;
            int taskCount = Math.min(currentTasks.size(), previousTasks.size());
            while (k < taskCount) {
                currentTasks.get(k).id = previousTasks.get(k).id;
                ++k;
            }
        }
    }

    private void assignIds(Target target) {
        String string = target.id = target.id == null || target.id.isBlank() ? IdGen.uid() : target.id;
        if (target.members != null) {
            for (Member member : target.members) {
                if (member.id != null && !member.id.isBlank()) continue;
                member.id = IdGen.uid();
            }
        }
        if (target.milestones != null) {
            for (Milestone milestone : target.milestones) {
                if (milestone.id == null || milestone.id.isBlank()) {
                    milestone.id = IdGen.uid();
                }
                if (milestone.tasks == null) continue;
                for (Task t : milestone.tasks) {
                    if (t.id == null || t.id.isBlank()) {
                        t.id = IdGen.uid();
                    }
                    if (t.subtasks == null) continue;
                    for (Subtask s : t.subtasks) {
                        if (s.id != null && !s.id.isBlank()) continue;
                        s.id = IdGen.uid();
                    }
                }
            }
        }
    }

    public String exportCsv(String userId, String targetId) {
        Target target = this.requireAccess(userId, targetId);
        StringBuilder sb = new StringBuilder();
        sb.append("StudyForge plan export\n");
        sb.append("Target,").append(csv(target.title)).append("\n");
        sb.append("Course module,").append(csv(target.courseModule)).append("\n");
        sb.append("Status,").append(target.status).append("\n");
        sb.append("Start date,").append(target.startDate == null ? "" : target.startDate).append("\n");
        sb.append("Deadline,").append(target.deadline == null ? "" : target.deadline).append("\n");
        sb.append("Progress,").append(this.progressService.stats(target).progress()).append("%\n");
        sb.append("\nMilestone,Task,Priority,Status,Assignee,Due date,Subtask,Subtask done\n");
        if (target.milestones != null) {
            for (Milestone m : target.milestones) {
                if (m.tasks == null || m.tasks.isEmpty()) {
                    sb.append(csv(m.title)).append(",,,,,").append(m.endDate == null ? "" : m.endDate).append(",\n");
                    continue;
                }
                for (Task t : m.tasks) {
                    String due = t.dueDate == null ? "" : t.dueDate.toString();
                    String base = csv(m.title) + "," + csv(t.title) + "," + (t.priority == null ? "" : t.priority.name()) + "," + (t.status == null ? "" : t.status.name()) + "," + csv(t.assigneeName) + "," + due;
                    if (t.subtasks == null || t.subtasks.isEmpty()) {
                        sb.append(base).append(",\n");
                        continue;
                    }
                    for (Subtask s : t.subtasks) {
                        sb.append(base).append(",").append(csv(s.body)).append(",").append(s.done).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    public String exportIcs(String userId, String targetId) {
        Target target = this.requireAccess(userId, targetId);
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//StudyForge//Study plan//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        if (target.startDate != null) {
            this.icsEvent(sb, target.id + "-start", "start", target.title + " (start)", target.startDate);
        }
        if (target.deadline != null) {
            this.icsEvent(sb, target.id + "-deadline", "deadline", target.title + " (deadline)", target.deadline);
        }
        if (target.milestones != null) {
            for (Milestone m : target.milestones) {
                if (m.endDate != null) {
                    this.icsEvent(sb, target.id + "-ms-" + m.id, "milestone", "Milestone: " + m.title, m.endDate);
                }
                if (m.tasks != null) {
                    for (Task t : m.tasks) {
                        if (t.dueDate != null) {
                            this.icsEvent(sb, target.id + "-t-" + t.id, "task", t.title + " (" + target.title + ")", t.dueDate);
                        }
                    }
                }
            }
        }
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private void icsEvent(StringBuilder sb, String uid, String type, String summary, LocalDate date) {
        String nowStamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC).format(Instant.now());
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(uid).append("@studyforge\r\n");
        sb.append("DTSTAMP:").append(nowStamp).append("\r\n");
        sb.append("DTSTART;VALUE=DATE:").append(date.toString().replace("-", "")).append("\r\n");
        sb.append("DTEND;VALUE=DATE:").append(date.plusDays(1L).toString().replace("-", "")).append("\r\n");
        sb.append("SUMMARY:").append(this.icsEsc(summary)).append("\r\n");
        sb.append("CATEGORIES:").append(type).append("\r\n");
        sb.append("END:VEVENT\r\n");
    }

    private String icsEsc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r", "").replace("\n", "\\n");
    }

    private String csv(String s) {
        if (s == null) return "";
        String value = s.replace("\"", "\"\"");
        return value.contains(",") || value.contains("\"") || value.contains("\n") ? "\"" + value + "\"" : value;
    }

    public TargetDtos.TargetView toView(Target target, String viewerId) {
        TargetDtos.ProgressStats stats = this.progressService.stats(target);
        List milestones = target.milestones == null ? List.of() : target.milestones.stream().map(TargetDtos::toMilestoneItem).toList();
        boolean isOwner = target.ownerId.equals(viewerId);
        String access = isOwner ? ACCESS_OWNER : (this.isViewer(target, viewerId) ? ACCESS_VIEWER : ACCESS_EDITOR);
        return new TargetDtos.TargetView(target.id, target.ownerId, this.ownerName(target.ownerId), access, target.title, target.description, target.courseModule, target.startDate, target.deadline, target.priority, target.status, target.source, target.aiNotes, target.members == null ? List.of() : target.members, milestones, stats, this.toIso(target.createdAt), this.toIso(target.updatedAt), target.activities == null ? List.of() : target.activities);
    }

    private boolean isViewer(Target target, String userId) {
        if (target.members == null) {
            return false;
        }
        for (Member m : target.members) {
            if (userId.equals(m.userId)) return m.role != null && m.role.equalsIgnoreCase("viewer");
        }
        return false;
    }

    private void requireEditable(String userId, Target target) {
        if (this.isViewer(target, userId)) {
            throw ApiException.forbidden("You have view-only access to this target");
        }
    }

    private void logActivity(Target target, String actorId, String type, String message) {
        if (target.activities == null) {
            target.activities = new ArrayList<Target.Activity>();
        }
        target.activities.add(new Target.Activity(type, message, actorId, this.nameOf(actorId)));
    }

    private String nameOf(String userId) {
        return this.userRepository.findById(userId).map(u -> u.name).orElse("Someone");
    }

    public Target requireOwner(String userId, String targetId) {
        Target target = (Target)this.targetRepository.findById(targetId).orElseThrow(() -> ApiException.notFound("Target not found"));
        if (!target.ownerId.equals(userId)) {
            throw ApiException.notFound("Target not found");
        }
        return target;
    }

    public Target requireAccess(String userId, String targetId) {
        Target target = (Target)this.targetRepository.findById(targetId).orElseThrow(() -> ApiException.notFound("Target not found"));
        if (target.ownerId.equals(userId) || target.collaboratorIds.contains(userId)) {
            return target;
        }
        throw ApiException.notFound("Target not found");
    }

    private List<Target> accessibleTargets(String userId) {
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        ArrayList<Target> all = new ArrayList<Target>();
        for (Target t : this.targetRepository.findByOwnerId(userId)) {
            if (!seen.add(t.id)) continue;
            all.add(t);
        }
        for (Target t : this.targetRepository.findByCollaboratorIdsContaining(userId)) {
            if (!seen.add(t.id)) continue;
            all.add(t);
        }
        return all;
    }

    private String ownerName(String ownerId) {
        return this.userRepository.findById(ownerId).map(u -> u.name).orElse(null);
    }

    private String toIso(Instant instant) {
        return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}

