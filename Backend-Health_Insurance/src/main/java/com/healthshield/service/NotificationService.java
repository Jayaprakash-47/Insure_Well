package com.healthshield.service;
import com.healthshield.entity.Notification;
import com.healthshield.enums.NotificationType;
import com.healthshield.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // One SSE emitter per logged-in user email
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String email) {
        // 1 hour timeout — frontend reconnects on timeout
        SseEmitter emitter = new SseEmitter(3600_000L);
        emitters.put(email, emitter);
        emitter.onCompletion(() -> emitters.remove(email));
        emitter.onTimeout(() -> emitters.remove(email));
        emitter.onError(e -> emitters.remove(email));

        // Send a ping immediately so browser confirms connection
        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (IOException e) {
            emitters.remove(email);
        }
        return emitter;
    }

    public void sendNotification(String recipientEmail, String message, NotificationType type) {
        // Persist to DB
        Notification notification = Notification.builder()
                .recipientEmail(recipientEmail)
                .message(message)
                .type(type)
                .build();
        notificationRepository.save(notification);

        // Push via SSE if user is connected
        SseEmitter emitter = emitters.get(recipientEmail);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                log.warn("SSE push failed for {}: {}", recipientEmail, e.getMessage());
                emitters.remove(recipientEmail);
            }
        }
    }

    public List<Notification> getAllNotifications(String email) {
        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(email);
    }

    public List<Notification> getUnreadNotifications(String email) {
        return notificationRepository.findByRecipientEmailAndIsReadFalseOrderByCreatedAtDesc(email);
    }

    public void markAllRead(String email) {
        notificationRepository.markAllAsReadByEmail(email);
    }

    public long getUnreadCount(String email) {
        return notificationRepository.countByRecipientEmailAndIsReadFalse(email);
    }
}