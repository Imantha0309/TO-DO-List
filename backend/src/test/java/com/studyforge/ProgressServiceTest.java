/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.studyforge.ProgressServiceTest
 *  com.studyforge.dto.TargetDtos$ProgressStats
 *  com.studyforge.model.Milestone
 *  com.studyforge.model.Subtask
 *  com.studyforge.model.Target
 *  com.studyforge.model.Task
 *  com.studyforge.model.TaskStatus
 *  com.studyforge.service.ProgressService
 *  org.assertj.core.api.Assertions
 *  org.junit.jupiter.api.Test
 */
package com.studyforge;

import com.studyforge.dto.TargetDtos;
import com.studyforge.model.Milestone;
import com.studyforge.model.Subtask;
import com.studyforge.model.Target;
import com.studyforge.model.Task;
import com.studyforge.model.TaskStatus;
import com.studyforge.service.ProgressService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ProgressServiceTest {
    private final ProgressService progressService = new ProgressService();

    ProgressServiceTest() {
    }

    private Target sample(boolean done1, boolean done2) {
        Target t = new Target("u1", "IT3070 project");
        Milestone m1 = new Milestone("m1", "Design");
        Task t1 = new Task("t1", "Write proposal");
        t1.status = done1 ? TaskStatus.DONE : TaskStatus.TODO;
        t1.subtasks.add(new Subtask("s1", "Draft"));
        t1.subtasks.add(new Subtask("s2", "Review"));
        ((Subtask)t1.subtasks.get((int)1)).done = true;
        Task t2 = new Task("t2", "Sketch architecture");
        t2.status = done2 ? TaskStatus.DONE : TaskStatus.TODO;
        m1.tasks.add(t1);
        m1.tasks.add(t2);
        t.milestones.add(m1);
        return t;
    }

    @Test
    void emptyTargetHasZeroProgress() {
        Target t = new Target("u1", "x");
        TargetDtos.ProgressStats s = this.progressService.stats(t);
        Assertions.assertThat((int)s.progress()).isZero();
        Assertions.assertThat((int)s.taskTotal()).isZero();
        Assertions.assertThat((boolean)this.progressService.isCompleted(t)).isFalse();
    }

    @Test
    void partialCompletionComputesPercent() {
        TargetDtos.ProgressStats s = this.progressService.stats(this.sample(true, false));
        Assertions.assertThat((int)s.taskTotal()).isEqualTo(2);
        Assertions.assertThat((int)s.taskDone()).isEqualTo(1);
        Assertions.assertThat((int)s.subtaskTotal()).isEqualTo(2);
        Assertions.assertThat((int)s.subtaskDone()).isEqualTo(1);
        Assertions.assertThat((int)s.progress()).isEqualTo(50);
        Assertions.assertThat((int)s.milestoneDone()).isZero();
        Assertions.assertThat((boolean)this.progressService.isCompleted(this.sample(true, false))).isFalse();
    }

    @Test
    void fullCompletionCompletesMilestoneAndTarget() {
        TargetDtos.ProgressStats s = this.progressService.stats(this.sample(true, true));
        Assertions.assertThat((int)s.progress()).isEqualTo(100);
        Assertions.assertThat((int)s.milestoneDone()).isEqualTo(1);
        Assertions.assertThat((boolean)this.progressService.isCompleted(this.sample(true, true))).isTrue();
    }
}

