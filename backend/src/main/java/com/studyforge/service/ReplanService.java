package com.studyforge.service;

import com.studyforge.dto.AiDtos;
import com.studyforge.dto.TargetDtos;
import com.studyforge.exception.ApiException;
import com.studyforge.model.Priority;
import com.studyforge.model.Target;
import com.studyforge.model.TargetSource;
import com.studyforge.model.TaskStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReplanService {
    private final TargetService targetService;
    private final AiPlanService aiPlanService;

    public ReplanService(TargetService targetService, AiPlanService aiPlanService) {
        this.targetService = targetService;
        this.aiPlanService = aiPlanService;
    }

    public TargetDtos.TargetView replan(String userId, String targetId) {
        Target target = this.targetService.requireAccess(userId, targetId);
        if (target.status == com.studyforge.model.TargetStatus.ARCHIVED) {
            throw ApiException.badRequest("Archived targets cannot be re-planned");
        }
        LocalDate today = LocalDate.now();
        LocalDate base = target.startDate != null && !target.startDate.isBefore(today) ? target.startDate : today;
        LocalDate deadline = target.deadline != null && !target.deadline.isBefore(base) ? target.deadline : null;

        Map<String, Object> known = new HashMap<String, Object>();
        known.put("startDate", base.toString());
        known.put("deadline", deadline == null ? null : deadline.toString());
        known.put("courseModule", target.courseModule);
        known.put("priority", target.priority == null ? null : target.priority.name());
        known.put("requirements", target.aiNotes);
        List<String> names = new ArrayList<String>();
        if (target.members != null) {
            for (com.studyforge.model.Member m : target.members) {
                if (m.name == null || m.name.isBlank()) continue;
                if (names.contains(m.name)) continue;
                names.add(m.name);
            }
        }
        known.put("members", names);
        known.put("memberNames", names);
        List<String> deliverables = new ArrayList<String>();
        if (target.milestones != null) {
            for (com.studyforge.model.Milestone m : target.milestones) {
                if (m.title != null && !m.title.isBlank()) {
                    deliverables.add(m.title);
                }
                if (m.tasks != null) {
                    for (com.studyforge.model.Task t : m.tasks) {
                        if (t.title != null && !t.title.isBlank()) {
                            deliverables.add(t.title);
                        }
                    }
                }
            }
        }
        known.put("deliverables", deliverables.subList(0, Math.min(deliverables.size(), 40)));

        String goal = buildGoal(target);

        AiDtos.AiPlanProposal proposal = this.aiPlanService.generatePlan(new AiDtos.PlanRequest(goal, known, Map.of()));
        List<TargetDtos.MilestoneRequest> milestones = new ArrayList<TargetDtos.MilestoneRequest>();
        if (proposal.milestones() != null) {
            for (AiDtos.AiMilestone am : proposal.milestones()) {
                List<TargetDtos.TaskRequest> tasks = new ArrayList<TargetDtos.TaskRequest>();
                if (am.tasks() != null) {
                    for (AiDtos.AiTaskSuggestion at : am.tasks()) {
                        LocalDate due = base.plusDays(Math.max(at.dueDayOffset() == null ? 0 : at.dueDayOffset(), 0));
                        List<TargetDtos.SubtaskRequest> subs = at.subtasks() == null ? List.of() : at.subtasks().stream().map(sb -> new TargetDtos.SubtaskRequest(sb, false)).toList();
                        tasks.add(new TargetDtos.TaskRequest(at.title(), at.description(), due, parsePriority(at.priority()), TaskStatus.TODO, at.assigneeName(), List.of(), subs));
                    }
                }
                LocalDate end = base.plusDays(Math.max(am.endDayOffset() == null ? 0 : am.endDayOffset(), 0));
                milestones.add(new TargetDtos.MilestoneRequest(am.title(), end, am.notes(), tasks));
            }
        }
        if (milestones.isEmpty()) {
            throw ApiException.badRequest("The AI did not produce a usable plan. Try again in a moment.");
        }
        String title = proposal.title() == null || proposal.title().isBlank() ? target.title : proposal.title();
        String description = proposal.description();
        String courseModule = proposal.courseModule() == null || proposal.courseModule().isBlank() ? target.courseModule : proposal.courseModule();
        String notes = proposal.notes() == null || proposal.notes().isBlank() ? "Plan rebuilt with AI. Dates are suggestions — review before committing." : proposal.notes();
        List<TargetDtos.MemberRequest> members = target.members == null ? List.of() : target.members.stream()
            .map(m -> new TargetDtos.MemberRequest(m.name, m.email, m.role, m.userId)).toList();
        TargetDtos.TargetRequest request = new TargetDtos.TargetRequest(title, description, courseModule, base, deadline, parsePriority(proposal.priority()), TargetSource.MANUAL, notes, members, milestones);
        TargetDtos.TargetView view = this.targetService.update(userId, targetId, request);
        this.targetService.appendActivity(userId, targetId, "Re-planned the plan with AI");
        return view;
    }

    private String buildGoal(Target target) {
        StringBuilder sb = new StringBuilder();
        sb.append("Target: ").append(target.title == null ? "Untitled" : target.title).append("\n");
        if (target.description != null && !target.description.isBlank()) {
            sb.append("Description: ").append(target.description).append("\n");
        }
        if (target.courseModule != null && !target.courseModule.isBlank()) {
            sb.append("Course module: ").append(target.courseModule).append("\n");
        }
        sb.append("Existing plan to refactor into a fresh, realistic schedule preserving the same overall scope.\n");
        return sb.toString();
    }

    private Priority parsePriority(String p) {
        if (p == null) return Priority.MEDIUM;
        try {
            return Priority.valueOf(p.trim().toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return Priority.MEDIUM;
        }
    }
}