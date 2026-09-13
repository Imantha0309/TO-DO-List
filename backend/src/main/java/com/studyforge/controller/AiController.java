/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.studyforge.controller;

import com.studyforge.dto.AiDtos;
import com.studyforge.service.AiPlanService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/ai"})
public class AiController {
    private final AiPlanService aiPlanService;

    public AiController(AiPlanService aiPlanService) {
        this.aiPlanService = aiPlanService;
    }

    @PostMapping(value={"/analyze"})
    public AiDtos.AnalyzeResponse analyze(@RequestBody AiDtos.AnalyzeRequest request) {
        return this.aiPlanService.analyze(request);
    }

    @PostMapping(value={"/plan"})
    public AiDtos.AiPlanProposal plan(@RequestBody AiDtos.PlanRequest request) {
        return this.aiPlanService.generatePlan(request);
    }

    @PostMapping(value={"/ping"})
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}

