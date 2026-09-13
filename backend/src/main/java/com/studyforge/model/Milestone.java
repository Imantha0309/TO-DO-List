/*
 * Decompiled with CFR 0.152.
 */
package com.studyforge.model;

import com.studyforge.model.Task;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Milestone {
    public String id;
    public String title;
    public LocalDate endDate;
    public String notes;
    public List<Task> tasks = new ArrayList<Task>();

    public Milestone() {
    }

    public Milestone(String id, String title) {
        this.id = id;
        this.title = title;
    }
}

