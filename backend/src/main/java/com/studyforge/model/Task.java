/*
 * Decompiled with CFR 0.152.
 */
package com.studyforge.model;

import com.studyforge.model.Priority;
import com.studyforge.model.Subtask;
import com.studyforge.model.TaskStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Task {
    public String id;
    public String title;
    public String description;
    public LocalDate dueDate;
    public Priority priority = Priority.MEDIUM;
    public TaskStatus status = TaskStatus.TODO;
    public String assigneeName;
    public List<String> dependsOn = new ArrayList<String>();
    public List<Subtask> subtasks = new ArrayList<Subtask>();

    public Task() {
    }

    public Task(String id, String title) {
        this.id = id;
        this.title = title;
    }
}

