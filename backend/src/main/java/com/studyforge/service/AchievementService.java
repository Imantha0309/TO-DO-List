/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.studyforge.service;

import com.studyforge.dto.AchievementDtos;
import com.studyforge.dto.TargetDtos;
import com.studyforge.model.Target;
import com.studyforge.model.User;
import com.studyforge.repository.TargetRepository;
import com.studyforge.repository.UserRepository;
import com.studyforge.service.ProgressService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AchievementService {
    private static final List<AchievementDefinition> CATALOG = List.of(
            new AchievementDefinition("first_target_created", "First Step", "Created your first target.", "target"),
            new AchievementDefinition("first_target_completed", "Mission Accomplished", "Completed your first target.", "trophy"),
            new AchievementDefinition("five_tasks_done", "Momentum", "Completed 5 tasks.", "bolt"),
            new AchievementDefinition("ten_tasks_done", "Task Machine", "Completed 10 tasks.", "rocket"),
            new AchievementDefinition("early_finish", "Ahead of Schedule", "Finished a target before its deadline.", "star"),
            new AchievementDefinition("milestone_master", "Milestone Master", "Completed 3 milestones.", "flag"));
    private final TargetRepository targetRepository;
    private final UserRepository userRepository;
    private final ProgressService progressService;

    public AchievementService(TargetRepository targetRepository, UserRepository userRepository, ProgressService progressService) {
        this.targetRepository = targetRepository;
        this.userRepository = userRepository;
        this.progressService = progressService;
    }

    public AchievementDtos.AchievementResponse getallFor(String userId) {
        User user = (User)this.userRepository.findById(userId).orElseThrow();
        List<Target> targets = this.targetRepository.findByOwnerId(userId);
        Set unlocked = user.unlocked.stream().map(u -> u.id).collect(Collectors.toSet());
        Set<String> satisfied = this.satisfied(targets);
        List<AchievementDtos.AchievementInfo> infos = CATALOG.stream().map(def -> new AchievementDtos.AchievementInfo(def.id(), def.name(), def.description(), def.icon(), unlocked.contains(def.id()), this.unlockedDate(user, def.id()))).toList();
        List<AchievementDtos.AchievementBrief> newly = satisfied.stream().filter(id -> !unlocked.contains(id)).sorted().map(this::brief).toList();
        if (!newly.isEmpty()) {
            for (String id2 : newly.stream().map(AchievementDtos.AchievementBrief::id).toList()) {
                user.unlocked.add(new User.UnlockedAchievement(id2, Instant.now()));
            }
            this.userRepository.save(user);
        }
        return new AchievementDtos.AchievementResponse(infos, newly);
    }

    public List<AchievementDtos.AchievementBrief> evaluate(String userId) {
        AchievementDtos.AchievementResponse response = this.getallFor(userId);
        if (response.newlyUnlocked().isEmpty()) {
            return List.of();
        }
        return response.newlyUnlocked();
    }

    private Set<String> satisfied(List<Target> all) {
        HashSet<String> flags = new HashSet<String>();
        List del = all.stream().filter(Objects::nonNull).toList();
        if (del.isEmpty()) {
            return flags;
        }
        if (all.size() > 0) {
            flags.add("first_target_created");
        }
        int totalDoneTasks = 0;
        int milestonesDone = 0;
        boolean anyCompleted = false;
        boolean anyEarly = false;
        for (Target t : all) {
            LocalDate completedDate;
            TargetDtos.ProgressStats s = this.progressService.stats(t);
            totalDoneTasks += s.taskDone();
            milestonesDone += s.milestoneDone();
            if (t.status == null || !t.status.name().equals("COMPLETED")) continue;
            anyCompleted = true;
            if (t.deadline == null || t.completedAt == null || (completedDate = t.completedAt.atZone(ZoneOffset.UTC).toLocalDate()).isAfter(t.deadline)) continue;
            anyEarly = true;
        }
        if (anyCompleted) {
            flags.add("first_target_completed");
        }
        if (totalDoneTasks >= 5) {
            flags.add("five_tasks_done");
        }
        if (totalDoneTasks >= 10) {
            flags.add("ten_tasks_done");
        }
        if (anyEarly) {
            flags.add("early_finish");
        }
        if (milestonesDone >= 3) {
            flags.add("milestone_master");
        }
        return flags;
    }

    private Instant unlockedDate(User user, String id) {
        return user.unlocked.stream().filter(u -> u.id.equals(id)).map(u -> u.unlockedAt).findFirst().orElse(null);
    }

    private AchievementDtos.AchievementBrief brief(String id) {
        return CATALOG.stream().filter(d -> d.id().equals(id)).map(d -> new AchievementDtos.AchievementBrief(d.id(), d.name(), d.description(), d.icon())).findFirst().orElse(null);
    }

    public record AchievementDefinition(String id, String name, String description, String icon) {
    }
}

