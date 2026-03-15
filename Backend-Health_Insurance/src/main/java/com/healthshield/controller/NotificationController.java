package com.healthshield.controller;

import com.healthshield.entity.Notification;
import com.healthshield.service.NotificationService;
import com.healthshield.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://localhost:62486"
})
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    // SSE uses query param token because EventSource API can't set headers
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String token, HttpServletResponse response) throws IOException {
        try {
            String email = jwtUtil.extractEmail(token);
            if (email == null || email.isBlank()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                SseEmitter emitter = new SseEmitter(0L);
                emitter.send(SseEmitter.event()
                        .name("auth-error")
                        .data("Session expired. Please log in again."));
                emitter.complete();
                return emitter;
            }
            return notificationService.subscribe(email);
        } catch (JwtException | IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            SseEmitter emitter = new SseEmitter(0L);
            emitter.send(SseEmitter.event()
                    .name("auth-error")
                    .data("Session expired. Please log in again."));
            emitter.complete();
            return emitter;
        }
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getUnread(Authentication auth) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(auth.getName()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll(Authentication auth) {
        return ResponseEntity.ok(notificationService.getAllNotifications(auth.getName()));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count",
                notificationService.getUnreadCount(auth.getName())));
    }

    @PutMapping("/mark-read")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        notificationService.markAllRead(auth.getName());
        return ResponseEntity.ok().build();
    }
}