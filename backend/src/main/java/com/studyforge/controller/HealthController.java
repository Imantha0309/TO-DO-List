package com.studyforge.controller;

import java.util.Map;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/health"})
public class HealthController {
    private final MongoTemplate mongoTemplate;

    public HealthController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        try {
            this.mongoTemplate.executeCommand("{ ping: 1 }");
            return ResponseEntity.ok(Map.of("status", "ok", "database", "connected"));
        }
        catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("status", "degraded", "database", "unreachable"));
        }
    }
}