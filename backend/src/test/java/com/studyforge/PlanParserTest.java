/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.studyforge.PlanParserTest
 *  com.studyforge.ai.PlanParser
 *  com.studyforge.dto.AiDtos$AiMilestone
 *  com.studyforge.dto.AiDtos$AiPlanProposal
 *  com.studyforge.dto.AiDtos$AiTaskSuggestion
 *  com.studyforge.exception.ApiException
 *  org.assertj.core.api.AbstractThrowableAssert
 *  org.assertj.core.api.Assertions
 *  org.junit.jupiter.api.Test
 */
package com.studyforge;

import com.studyforge.ai.PlanParser;
import com.studyforge.dto.AiDtos;
import com.studyforge.exception.ApiException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class PlanParserTest {
    private final PlanParser parser = new PlanParser();
    private final String validJson = "{\n  \"title\": \"IT3070 Group Project\",\n  \"description\": \"Build a web app with a team\",\n  \"courseModule\": \"IT3070\",\n  \"priority\": \"HIGH\",\n  \"notes\": \"Dense but realistic plan\",\n  \"milestones\": [\n    {\"title\": \"Kickoff\", \"endDayOffset\": 7, \"notes\": \"planning\", \"tasks\": [\n      {\"title\": \"Write proposal\", \"description\": \"One-pager\",\n       \"dueDayOffset\": 3, \"priority\": \"HIGH\", \"assigneeName\": \"Member 2\",\n       \"subtasks\": [\"Draft\", \"Review\"]},\n      {\"title\": \"Setup repo\", \"description\": \"\", \"dueDayOffset\": 4,\n       \"priority\": \"low\", \"assigneeName\": null, \"subtasks\": []}\n    ]},\n    {\"title\": \"Build core\", \"endDayOffset\": 20, \"notes\": \"\", \"tasks\": [\n      {\"title\": \"Backend API\", \"description\": \"\", \"dueDayOffset\": 15,\n       \"priority\": \"MEDIUM\", \"assigneeName\": \"Member 3\", \"subtasks\": [\"Routes\", \"Tests\"]}\n    ]}\n  ]\n}\n";

    PlanParserTest() {
    }

    @Test
    void parsesValidPlanAndComputesDatesFromOffsets() {
        LocalDate start = LocalDate.of(2026, 9, 13);
        AiDtos.AiPlanProposal plan = this.parser.parse("{\n  \"title\": \"IT3070 Group Project\",\n  \"description\": \"Build a web app with a team\",\n  \"courseModule\": \"IT3070\",\n  \"priority\": \"HIGH\",\n  \"notes\": \"Dense but realistic plan\",\n  \"milestones\": [\n    {\"title\": \"Kickoff\", \"endDayOffset\": 7, \"notes\": \"planning\", \"tasks\": [\n      {\"title\": \"Write proposal\", \"description\": \"One-pager\",\n       \"dueDayOffset\": 3, \"priority\": \"HIGH\", \"assigneeName\": \"Member 2\",\n       \"subtasks\": [\"Draft\", \"Review\"]},\n      {\"title\": \"Setup repo\", \"description\": \"\", \"dueDayOffset\": 4,\n       \"priority\": \"low\", \"assigneeName\": null, \"subtasks\": []}\n    ]},\n    {\"title\": \"Build core\", \"endDayOffset\": 20, \"notes\": \"\", \"tasks\": [\n      {\"title\": \"Backend API\", \"description\": \"\", \"dueDayOffset\": 15,\n       \"priority\": \"MEDIUM\", \"assigneeName\": \"Member 3\", \"subtasks\": [\"Routes\", \"Tests\"]}\n    ]}\n  ]\n}\n", start, null, Set.of("Member 2", "Member 3"));
        Assertions.assertThat((String)plan.title()).isEqualTo("IT3070 Group Project");
        Assertions.assertThat((List)plan.milestones()).hasSize(2);
        Assertions.assertThat((List)((AiDtos.AiMilestone)plan.milestones().get(0)).tasks()).hasSize(2);
        Assertions.assertThat((String)plan.startDate()).isEqualTo("2026-09-13");
        Assertions.assertThat((Integer)((AiDtos.AiTaskSuggestion)((AiDtos.AiMilestone)plan.milestones().get(0)).tasks().get(0)).dueDayOffset()).isEqualTo(3);
        Assertions.assertThat((String)plan.deadline()).isEqualTo(start.plusDays(20L).toString());
        Assertions.assertThat((String)((AiDtos.AiTaskSuggestion)((AiDtos.AiMilestone)plan.milestones().get(0)).tasks().get(1)).priority()).isEqualTo("LOW");
    }

    @Test
    void keepsRequestedDeadline() {
        LocalDate start = LocalDate.of(2026, 9, 13);
        LocalDate deadline = LocalDate.of(2026, 10, 20);
        AiDtos.AiPlanProposal plan = this.parser.parse("{\n  \"title\": \"IT3070 Group Project\",\n  \"description\": \"Build a web app with a team\",\n  \"courseModule\": \"IT3070\",\n  \"priority\": \"HIGH\",\n  \"notes\": \"Dense but realistic plan\",\n  \"milestones\": [\n    {\"title\": \"Kickoff\", \"endDayOffset\": 7, \"notes\": \"planning\", \"tasks\": [\n      {\"title\": \"Write proposal\", \"description\": \"One-pager\",\n       \"dueDayOffset\": 3, \"priority\": \"HIGH\", \"assigneeName\": \"Member 2\",\n       \"subtasks\": [\"Draft\", \"Review\"]},\n      {\"title\": \"Setup repo\", \"description\": \"\", \"dueDayOffset\": 4,\n       \"priority\": \"low\", \"assigneeName\": null, \"subtasks\": []}\n    ]},\n    {\"title\": \"Build core\", \"endDayOffset\": 20, \"notes\": \"\", \"tasks\": [\n      {\"title\": \"Backend API\", \"description\": \"\", \"dueDayOffset\": 15,\n       \"priority\": \"MEDIUM\", \"assigneeName\": \"Member 3\", \"subtasks\": [\"Routes\", \"Tests\"]}\n    ]}\n  ]\n}\n", start, deadline, Set.of());
        Assertions.assertThat((String)plan.deadline()).isEqualTo("2026-10-20");
    }

    @Test
    void emptyMilestonesThrowsFriendlyError() {
        LocalDate start = LocalDate.of(2026, 9, 13);
        ((AbstractThrowableAssert)Assertions.assertThatThrownBy(() -> this.parser.parse("{\"title\":\"x\",\"milestones\":[]}", start, null, Set.of())).isInstanceOf(ApiException.class)).hasMessageContaining("empty");
    }

    @Test
    void malformedJsonThrowsFriendlyError() {
        LocalDate start = LocalDate.of(2026, 9, 13);
        ((AbstractThrowableAssert)Assertions.assertThatThrownBy(() -> this.parser.parse("not json at all", start, null, Set.of())).isInstanceOf(ApiException.class)).hasMessageContaining("malformed");
    }

    @Test
    void missingFieldsAreDefaultedNotCrashed() {
        LocalDate start = LocalDate.of(2026, 9, 13);
        String json = "{\"milestones\":[{\"tasks\":[{\"title\":\"T\"}]}]}";
        AiDtos.AiPlanProposal plan = this.parser.parse(json, start, null, Set.of());
        Assertions.assertThat((String)plan.title()).isEqualTo("Unnamed Target");
        Assertions.assertThat((Integer)((AiDtos.AiMilestone)plan.milestones().get(0)).endDayOffset()).isEqualTo(7);
        Assertions.assertThat((String)((AiDtos.AiTaskSuggestion)((AiDtos.AiMilestone)plan.milestones().get(0)).tasks().get(0)).priority()).isEqualTo("MEDIUM");
    }
}

