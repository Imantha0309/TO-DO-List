/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.studyforge.service;

import com.studyforge.dto.DashboardDtos;
import com.studyforge.dto.TargetDtos;
import com.studyforge.model.Milestone;
import com.studyforge.model.Target;
import com.studyforge.model.TargetStatus;
import com.studyforge.model.Task;
import com.studyforge.model.TaskStatus;
import com.studyforge.repository.TargetRepository;
import com.studyforge.service.ProgressService;
import com.studyforge.service.TargetService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final TargetRepository targetRepository;
    private final TargetService targetService;
    private final ProgressService progressService;

    public DashboardService(TargetRepository targetRepository, TargetService targetService, ProgressService progressService) {
        this.targetRepository = targetRepository;
        this.targetService = targetService;
        this.progressService = progressService;
    }

    public DashboardDtos.DashboardResponse forUser(String userId) {
        List<Target> all = this.accessibleTargets(userId);
        LocalDate today = LocalDate.now();
        int active = 0;
        int completed = 0;
        int totalTasks = 0;
        int doneTasks = 0;
        double overallSum = 0.0;
        ArrayList<DashboardDtos.TodayTask> todayTasks = new ArrayList<DashboardDtos.TodayTask>();
        ArrayList<DashboardDtos.UpcomingDeadline> upcoming = new ArrayList<DashboardDtos.UpcomingDeadline>();
        for (Target t2 : all) {
            long remaining;
            if (t2.status == TargetStatus.ARCHIVED) continue;
            if (t2.status == TargetStatus.COMPLETED) {
                ++completed;
            } else {
                ++active;
            }
            TargetDtos.ProgressStats stats = this.progressService.stats(t2);
            totalTasks += stats.taskTotal();
            doneTasks += stats.taskDone();
            overallSum += (double)stats.progress();
            if (t2.deadline != null && t2.status != TargetStatus.COMPLETED && (remaining = ChronoUnit.DAYS.between(today, t2.deadline)) <= 14L && remaining >= 0L) {
                upcoming.add(new DashboardDtos.UpcomingDeadline(t2.id, t2.title, t2.deadline.toString(), (int)remaining));
            }
            for (Milestone m : t2.milestones) {
                for (Task task : m.tasks) {
                    if (task.status == TaskStatus.DONE || task.dueDate == null || task.dueDate.isAfter(today) || todayTasks.size() >= 8) continue;
                    todayTasks.add(new DashboardDtos.TodayTask(task.id, t2.id, t2.title, m.id, task.title, task.dueDate.toString(), task.priority == null ? null : task.priority.name(), task.assigneeName));
                }
            }
        }
        todayTasks.sort(Comparator.comparing(DashboardDtos.TodayTask::dueDate));
        upcoming.sort(Comparator.comparing(DashboardDtos.UpcomingDeadline::remainingDays));
        List<TargetDtos.TargetView> recent = all.stream().filter(t -> t.status != TargetStatus.ARCHIVED).sorted(Comparator.comparing(t -> ((Target)t).updatedAt).reversed()).limit(5L).map(t -> this.targetService.toView((Target)t, userId)).toList();
        int denominator = Math.max(active + completed, 0);
        double overall = denominator == 0 ? 0.0 : (double)Math.round(overallSum / (double)denominator * 10.0) / 10.0;
        List<DashboardDtos.StatItem> quickStats = List.of(
                new DashboardDtos.StatItem("Active targets", active),
                new DashboardDtos.StatItem("Completed", completed),
                new DashboardDtos.StatItem("Tasks done", doneTasks),
                new DashboardDtos.StatItem("Overall progress", (int)Math.round(overall)));
        return new DashboardDtos.DashboardResponse(all.size(), active, completed, totalTasks, doneTasks, overall, todayTasks, upcoming, recent, quickStats);
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
}

