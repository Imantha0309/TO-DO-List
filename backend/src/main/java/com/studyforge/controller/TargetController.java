/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.studyforge.controller;

import com.studyforge.dto.TargetDtos;
import com.studyforge.service.AchievementService;
import com.studyforge.service.ReplanService;
import com.studyforge.service.TargetService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/targets"})
public class TargetController {
    private final TargetService targetService;
    private final AchievementService achievementService;
    private final ReplanService replanService;

    public TargetController(TargetService targetService, AchievementService achievementService, ReplanService replanService) {
        this.targetService = targetService;
        this.achievementService = achievementService;
        this.replanService = replanService;
    }

    @GetMapping
    public List<TargetDtos.TargetView> list(Authentication authentication) {
        return this.targetService.list(authentication.getName());
    }

    @GetMapping(value={"/{id}"})
    public TargetDtos.TargetView get(Authentication authentication, @PathVariable String id) {
        return this.targetService.get(authentication.getName(), id);
    }

    @PostMapping
    public ResponseEntity<TargetDtos.TargetResponse> create(Authentication authentication, @RequestBody TargetDtos.TargetRequest request) {
        String userId = authentication.getName();
        TargetDtos.TargetView view = this.targetService.create(userId, request);
        return ResponseEntity.ok(new TargetDtos.TargetResponse(view, this.achievementService.evaluate(userId)));
    }

    @PutMapping(value={"/{id}"})
    public ResponseEntity<TargetDtos.TargetResponse> update(Authentication authentication, @PathVariable String id, @RequestBody TargetDtos.TargetRequest request) {
        String userId = authentication.getName();
        TargetDtos.TargetView view = this.targetService.update(userId, id, request);
        return ResponseEntity.ok(new TargetDtos.TargetResponse(view, this.achievementService.evaluate(userId)));
    }

    @DeleteMapping(value={"/{id}"})
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable String id) {
        this.targetService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value={"/{id}/members"})
    public TargetDtos.TargetView addMember(Authentication authentication, @PathVariable String id, @RequestBody TargetDtos.MemberRequest request) {
        return this.targetService.addMember(authentication.getName(), id, request);
    }

    @DeleteMapping(value={"/{id}/members/{memberId}"})
    public TargetDtos.TargetView removeMember(Authentication authentication, @PathVariable String id, @PathVariable String memberId) {
        return this.targetService.removeMember(authentication.getName(), id, memberId);
    }

    @PostMapping(value={"/{id}/collaborators"})
    public TargetDtos.TargetView addCollaborator(Authentication authentication, @PathVariable String id, @RequestBody TargetDtos.CollaboratorRequest request) {
        return this.targetService.addCollaborator(authentication.getName(), id, request.email());
    }

    @DeleteMapping(value={"/{id}/collaborators/{collaboratorUserId}"})
    public TargetDtos.TargetView removeCollaborator(Authentication authentication, @PathVariable String id, @PathVariable String collaboratorUserId) {
        return this.targetService.removeCollaborator(authentication.getName(), id, collaboratorUserId);
    }

    @PutMapping(value={"/{id}/collaborators/{collaboratorUserId}/role"})
    public TargetDtos.TargetView setCollaboratorRole(Authentication authentication, @PathVariable String id, @PathVariable String collaboratorUserId, @RequestBody TargetDtos.CollaboratorRoleRequest request) {
        return this.targetService.setCollaboratorRole(authentication.getName(), id, collaboratorUserId, request.role());
    }

    @PostMapping(value={"/{id}/replan"})
    public ResponseEntity<TargetDtos.TargetResponse> replan(Authentication authentication, @PathVariable String id) {
        String userId = authentication.getName();
        TargetDtos.TargetView view = this.replanService.replan(userId, id);
        return ResponseEntity.ok(new TargetDtos.TargetResponse(view, this.achievementService.evaluate(userId)));
    }

    @GetMapping(value={"/{id}/export/csv"}, produces={"text/csv"})
    public ResponseEntity<byte[]> exportCsv(Authentication authentication, @PathVariable String id) {
        byte[] data = this.targetService.exportCsv(authentication.getName(), id).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plan-" + id + ".csv").contentType(MediaType.parseMediaType("text/csv")).body(data);
    }

    @GetMapping(value={"/{id}/export/ics"}, produces={"text/calendar"})
    public ResponseEntity<byte[]> exportIcs(Authentication authentication, @PathVariable String id) {
        byte[] data = this.targetService.exportIcs(authentication.getName(), id).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plan-" + id + ".ics").contentType(MediaType.parseMediaType("text/calendar")).body(data);
    }
}

