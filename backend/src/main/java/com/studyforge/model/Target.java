/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.annotation.Id
 *  org.springframework.data.mongodb.core.index.Indexed
 *  org.springframework.data.mongodb.core.mapping.Document
 */
package com.studyforge.model;

import com.studyforge.model.Member;
import com.studyforge.model.Milestone;
import com.studyforge.model.Priority;
import com.studyforge.model.TargetSource;
import com.studyforge.model.TargetStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(value="targets")
public class Target {
    @Id
    public String id;
    @Indexed
    public String ownerId;
    public String title;
    public String description;
    public String courseModule;
    public LocalDate startDate;
    public LocalDate deadline;
    public Priority priority = Priority.MEDIUM;
    public TargetStatus status = TargetStatus.ACTIVE;
    public TargetSource source = TargetSource.MANUAL;
    public String aiNotes;
    public List<Member> members = new ArrayList<Member>();
    public List<String> collaboratorIds = new ArrayList<String>();
    public List<Milestone> milestones = new ArrayList<Milestone>();
    public List<Activity> activities = new ArrayList<Activity>();
    public Instant completedAt;
    public Instant createdAt = Instant.now();
    public Instant updatedAt = Instant.now();

    public Target() {
    }

    public Target(String ownerId, String title) {
        this.ownerId = ownerId;
        this.title = title;
    }

    public static class Activity {
        public String type;
        public String message;
        public String actorId;
        public String actorName;
        public Instant at = Instant.now();

        public Activity() {
        }

        public Activity(String type, String message, String actorId, String actorName) {
            this.type = type;
            this.message = message;
            this.actorId = actorId;
            this.actorName = actorName;
        }
    }
}

