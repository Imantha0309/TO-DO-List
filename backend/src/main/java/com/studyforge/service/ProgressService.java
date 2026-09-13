/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Service
 */
package com.studyforge.service;

import com.studyforge.dto.TargetDtos;
import com.studyforge.model.Milestone;
import com.studyforge.model.Subtask;
import com.studyforge.model.Target;
import com.studyforge.model.Task;
import com.studyforge.model.TaskStatus;
import org.springframework.stereotype.Service;

@Service
public class ProgressService {
    public TargetDtos.ProgressStats stats(Target target) {
        int taskTotal = 0;
        int taskDone = 0;
        int subtaskTotal = 0;
        int subtaskDone = 0;
        int milestoneDone = 0;
        if (target.milestones != null) {
            for (Milestone m : target.milestones) {
                int mTotal;
                int mDone = 0;
                int n = mTotal = m.tasks == null ? 0 : m.tasks.size();
                if (m.tasks != null) {
                    for (Task t : m.tasks) {
                        if (!TaskStatus.DONE.equals((Object)t.status)) continue;
                        ++mDone;
                        ++taskDone;
                        if (t.subtasks == null) continue;
                        for (Subtask s : t.subtasks) {
                            if (!s.done) continue;
                            ++subtaskDone;
                        }
                    }
                }
                if (m.tasks != null) {
                    for (Task t : m.tasks) {
                        subtaskTotal += t.subtasks == null ? 0 : t.subtasks.size();
                    }
                }
                taskTotal += mTotal;
                if (mTotal <= 0 || mDone != mTotal) continue;
                ++milestoneDone;
            }
        }
        int progress = taskTotal == 0 ? 0 : (int)Math.round((double)taskDone * 100.0 / (double)taskTotal);
        return new TargetDtos.ProgressStats(taskTotal, taskDone, subtaskTotal, subtaskDone, target.milestones == null ? 0 : target.milestones.size(), milestoneDone, progress);
    }

    public boolean isCompleted(Target target) {
        TargetDtos.ProgressStats s = this.stats(target);
        return s.taskTotal() > 0 && s.taskDone() == s.taskTotal();
    }
}

