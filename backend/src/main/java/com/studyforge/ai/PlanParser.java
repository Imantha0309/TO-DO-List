/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  org.springframework.stereotype.Component
 */
package com.studyforge.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.dto.AiDtos;
import com.studyforge.exception.ApiException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PlanParser {
    private final ObjectMapper mapper = new ObjectMapper();

    public AiDtos.AiPlanProposal parse(String json, LocalDate start, LocalDate requestedDeadline, Set<String> memberNames) {
        JsonNode root;
        try {
            root = this.mapper.readTree(json);
        }
        catch (Exception e) {
            throw ApiException.badRequest("The AI returned malformed JSON. Please try again or choose manual mode.");
        }
        if (root == null || !root.isObject()) {
            throw ApiException.badRequest("The AI returned an unreadable plan. Please try again.");
        }
        String title = this.text(root, "title");
        if (title == null || title.isBlank()) {
            title = "Unnamed Target";
        }
        String startDate = start.toString();
        int maxOffset = 0;
        ArrayList<AiDtos.AiMilestone> milestones = new ArrayList<AiDtos.AiMilestone>();
        JsonNode msNode = root.get("milestones");
        if (msNode != null && msNode.isArray()) {
            int index = 0;
            int taskCounter = 0;
            for (JsonNode m : msNode) {
                if (!m.isObject()) continue;
                ++index;
                String mTitle = this.text(m, "title");
                if (mTitle == null || mTitle.isBlank()) {
                    mTitle = "Milestone " + index;
                }
                Integer endOffset = this.clampOffset(this.intValue(m, "endDayOffset"), index * 7, 1, 365);
                maxOffset = Math.max(maxOffset, endOffset);
                ArrayList<AiDtos.AiTaskSuggestion> tasks = new ArrayList<AiDtos.AiTaskSuggestion>();
                JsonNode tasksNode = m.get("tasks");
                if (tasksNode != null && tasksNode.isArray()) {
                    for (JsonNode t : tasksNode) {
                        if (!t.isObject() || this.text(t, "title") == null) continue;
                        Integer dueOffset = this.clampOffset(this.intValue(t, "dueDayOffset"), endOffset, 0, endOffset);
                        maxOffset = Math.max(maxOffset, dueOffset);
                        String priority = this.sanitizePriority(this.text(t, "priority"));
                        String assignee = this.sanitizeAssignee(this.text(t, "assigneeName"), memberNames);
                        ArrayList<String> subtasks = new ArrayList<String>();
                        JsonNode subs = t.get("subtasks");
                        if (subs != null && subs.isArray()) {
                            for (JsonNode s : subs) {
                                String body = s.asText(null);
                                if (body == null || body.isBlank() || subtasks.size() >= 6) continue;
                                subtasks.add(body.trim());
                            }
                        }
                        tasks.add(new AiDtos.AiTaskSuggestion("tmp-" + taskCounter++, this.text(t, "title"), this.text(t, "description"), dueOffset, priority, assignee, subtasks));
                    }
                }
                milestones.add(new AiDtos.AiMilestone("tmp-m" + index, (String)mTitle, endOffset, this.text(m, "notes"), tasks));
            }
        }
        if (milestones.isEmpty()) {
            throw ApiException.badRequest("The AI returned an empty plan. Please try again or choose manual mode.");
        }
        LocalDate effectiveDeadline = requestedDeadline != null ? requestedDeadline : start.plusDays(maxOffset);
        String priority = this.sanitizePriority(this.text(root, "priority"));
        Object notes = this.text(root, "notes");
        if (requestedDeadline == null) {
            notes = ((String)(notes == null ? "" : (String)notes + " ")).trim() + "A deadline wasn't specified, so it was set to " + String.valueOf(effectiveDeadline) + ".";
        }
        return new AiDtos.AiPlanProposal(title, this.text(root, "description"), this.text(root, "courseModule"), startDate, effectiveDeadline.toString(), priority, (String)notes, milestones);
    }

    private String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n == null || n.isNull() ? null : n.asText(null);
    }

    private Integer intValue(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.asInt();
        }
        try {
            return Integer.parseInt(n.asText().trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer clampOffset(Integer value, Integer fallback, int min, int max) {
        int v = value != null ? value : (fallback != null ? fallback : min);
        if (v < min) {
            v = min;
        }
        if (v > max) {
            v = max;
        }
        return v;
    }

    private String sanitizePriority(String p) {
        if (p == null) {
            return "MEDIUM";
        }
        String up = p.toUpperCase();
        if (up.equals("LOW") || up.equals("MEDIUM") || up.equals("HIGH")) {
            return up;
        }
        return "MEDIUM";
    }

    private String sanitizeAssignee(String assignee, Set<String> memberNames) {
        if (assignee == null || assignee.isBlank()) {
            return null;
        }
        String a = assignee.trim();
        if (memberNames.isEmpty()) {
            return a;
        }
        for (String m : memberNames) {
            if (!m.equalsIgnoreCase(a)) continue;
            return a;
        }
        return a;
    }
}

