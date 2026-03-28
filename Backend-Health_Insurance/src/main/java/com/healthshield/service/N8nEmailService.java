package com.healthshield.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
@Service
public class N8nEmailService {

    @Value("${n8n.webhook.email}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sends an email via n8n webhook instead of direct Gmail SMTP.
     * n8n handles the actual email delivery.
     *
     * @param to      Recipient email address
     * @param subject Email subject line
     * @param body    Email body (plain text or HTML)
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            Map<String, String> payload = Map.of(
                "to", to,
                "subject", subject,
                "body", body
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Uncomment below when Header Auth is enabled in n8n
            // headers.set("X-Webhook-Secret", webhookSecret);

            restTemplate.postForObject(
                webhookUrl,
                new HttpEntity<>(payload, headers),
                String.class
            );

            log.info("✅ Email sent via n8n to: {}", to);

        } catch (Exception e) {
            log.error("❌ n8n email failed for {}: {}", to, e.getMessage());
            throw new RuntimeException("Email delivery failed via n8n");
        }
    }
}
