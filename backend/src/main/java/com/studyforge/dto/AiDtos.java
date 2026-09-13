/*
 * Decompiled with CFR 0.152.
 */
package com.studyforge.dto;

import java.util.List;
import java.util.Map;

public class AiDtos {

    public record AiPlanProposal(String title, String description, String courseModule, String startDate, String deadline, String priority, String notes, List<AiMilestone> milestones) {
    }

    public record AiMilestone(String id, String title, Integer endDayOffset, String notes, List<AiTaskSuggestion> tasks) {
    }

    public record AiTaskSuggestion(String id, String title, String description, Integer dueDayOffset, String priority, String assigneeName, List<String> subtasks) {
    }

    public record PlanRequest(String goal, Map<String, Object> extracted, Map<String, Object> answers) {
    }

    public record AnalyzeResponse(Map<String, Object> extracted, List<SurveyQuestion> questions) {
    }

    public record SurveyQuestion(String field, String question, String type, boolean required, List<String> options) {
    }

    public record AnalyzeRequest(String text, String pdfData, String pdfName) {
    }
}

