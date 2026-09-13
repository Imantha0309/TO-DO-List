/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.ResponseEntity
 *  org.springframework.http.converter.HttpMessageNotReadableException
 *  org.springframework.security.access.AccessDeniedException
 *  org.springframework.web.bind.MethodArgumentNotValidException
 *  org.springframework.web.bind.annotation.ExceptionHandler
 *  org.springframework.web.bind.annotation.RestControllerAdvice
 *  org.springframework.web.servlet.resource.NoResourceFoundException
 */
package com.studyforge.exception;

import com.studyforge.exception.ApiException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value={ApiException.class})
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return this.build(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(value={HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return this.build(HttpStatus.BAD_REQUEST, "Request body is not valid JSON or does not match the expected format. Check fields like milestones, tasks, subtasks and members (members must be objects with a \"name\").");
    }

    @ExceptionHandler(value={MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream().findFirst().map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).orElse("Invalid request");
        return this.build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(value={AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return this.build(HttpStatus.FORBIDDEN, "You don't have access to this resource");
    }

    @ExceptionHandler(value={NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        return this.build(HttpStatus.NOT_FOUND, "Not found");
    }

    @ExceptionHandler(value={Exception.class})
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.warn("Unhandled exception", (Throwable)ex);
        return this.build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Try again, and if it keeps happening report the issue.");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        HashMap<String, Object> body = new HashMap<String, Object>();
        body.put("error", message);
        body.put("status", status.value());
        return ResponseEntity.status((HttpStatusCode)status).body(body);
    }
}

