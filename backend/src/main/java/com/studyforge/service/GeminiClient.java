/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.web.client.RestTemplateBuilder
 *  org.springframework.http.HttpEntity
 *  org.springframework.http.HttpHeaders
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.MediaType
 *  org.springframework.http.ResponseEntity
 *  org.springframework.stereotype.Service
 *  org.springframework.util.MultiValueMap
 *  org.springframework.web.client.RestClientResponseException
 *  org.springframework.web.client.RestTemplate
 */
package com.studyforge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyforge.exception.ApiException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiClient {
    private final String apiKey;
    private final String model;
    private final long maxUploadBytes;
    private final RestTemplate restTemplate;

    public GeminiClient(@Value(value="${studyforge.gemini.api-key}") String apiKey, @Value(value="${studyforge.gemini.model}") String model, @Value(value="${studyforge.gemini.max-upload-bytes}") long maxUploadBytes, RestTemplateBuilder builder) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxUploadBytes = maxUploadBytes;
        this.restTemplate = builder.setConnectTimeout(Duration.ofSeconds(15L)).setReadTimeout(Duration.ofSeconds(90L)).build();
    }

    public String generateContent(String systemInstruction, String userPrompt, String pdfData, String mimeType) {
        if (this.apiKey.isBlank()) {
            throw ApiException.badRequest("Gemini API key is not configured. Add GEMINI_API_KEY to continue.");
        }
        ArrayList<Map> parts = new ArrayList<Map>();
        parts.add(Map.of((Object)"text", (Object)userPrompt));
        if (pdfData != null && !pdfData.isBlank()) {
            if ((long)pdfData.length() > this.maxUploadBytes * 2L) {
                throw ApiException.badRequest("Uploaded file is too large. Maximum size is " + this.maxUploadBytes / 1024L / 1024L + " MB.");
            }
            String effectiveMime = mimeType == null ? "application/pdf" : mimeType;
            parts.add(Map.of((Object)"inlineData", (Object)Map.of((Object)"mimeType", (Object)effectiveMime, (Object)"data", (Object)pdfData)));
        }
        Map contents = Map.of((Object)"role", (Object)"user", (Object)"parts", parts);
        Map generationConfig = Map.of((Object)"responseMimeType", (Object)"application/json", (Object)"temperature", (Object)0.4);
        HashMap<String, Object> body = new HashMap<String, Object>();
        body.put("contents", List.of((Object)contents));
        body.put("generationConfig", generationConfig);
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            body.put("systemInstruction", Map.of((Object)"parts", (Object)List.of((Object)Map.of((Object)"text", (Object)systemInstruction))));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity entity = new HttpEntity(body, (MultiValueMap)headers);
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + this.model + ":generateContent?key=" + this.apiKey;
            ResponseEntity response = this.restTemplate.postForEntity(url, (Object)entity, Map.class, new Object[0]);
            Map root = (Map)response.getBody();
            if (root == null) {
                throw ApiException.badRequest("Empty response from AI service");
            }
            Object candidates = root.get("candidates");
            if (!(candidates instanceof List) || ((List)candidates).isEmpty()) {
                throw ApiException.badRequest(this.extractApiError(root));
            }
            Map candidate = (Map)((List)candidates).get(0);
            Map content = (Map)candidate.get("content");
            List partList = (List)content.get("parts");
            Map part = (Map)partList.get(0);
            Object text = part.get("text");
            if (text == null) {
                throw ApiException.badRequest("AI returned no text");
            }
            return text.toString();
        }
        catch (ApiException e) {
            throw e;
        }
        catch (RestClientResponseException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, this.buildHttpErrorMessage(e.getStatusCode().value(), e.getStatusText(), e.getResponseBodyAsString()));
        }
        catch (Exception e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable: " + this.simpleMsg(e));
        }
    }

    private String buildHttpErrorMessage(int code, String statusText, String body) {
        if (code == 401) {
            return "Gemini API key is invalid or missing access. Check GEMINI_API_KEY and make sure it's an API key (starts with AIza) from https://aistudio.google.com/apikey";
        }
        if (code == 403) {
            return "Gemini API key is not allowed to use this model. Check billing and model access.";
        }
        if (code == 404) {
            return "Gemini model '" + this.model + "' was not found or has been retired. Update GEMINI_MODEL.";
        }
        if (code == 429) {
            return "Gemini rate limit reached. Wait a moment and try again.";
        }
        Object reason = body != null && !body.isBlank() ? this.extractMessage(body) : (statusText == null || statusText.isBlank() ? "HTTP " + code : statusText);
        return "AI service error (" + code + "): " + (String)reason;
    }

    private String extractMessage(String body) {
        try {
            JsonNode node = new ObjectMapper().readTree(body);
            String msg = node.path("error").path("message").asText(null);
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
            return body.length() > 200 ? body.substring(0, 200) : body;
        }
        catch (Exception e) {
            return body.length() > 200 ? body.substring(0, 200) : body;
        }
    }

    private String extractApiError(Map<?, ?> root) {
        try {
            Object blockReason;
            Object promptFeedback = root.get("promptFeedback");
            if (promptFeedback instanceof Map && (blockReason = ((Map)promptFeedback).get("blockReason")) != null) {
                return "AI refused the request (" + String.valueOf(blockReason) + "). Try rephrasing your goal.";
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return "AI could not generate a response. Please try again.";
    }

    private String simpleMsg(Exception e) {
        String m = e.getMessage();
        if (m == null) {
            return e.getClass().getSimpleName();
        }
        return m.length() > 300 ? m.substring(0, 300) : m;
    }

    public long getMaxUploadBytes() {
        return this.maxUploadBytes;
    }
}

