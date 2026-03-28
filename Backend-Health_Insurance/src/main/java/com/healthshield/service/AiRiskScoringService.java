package com.healthshield.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthshield.entity.Policy;
import com.healthshield.entity.PolicyMember;
import com.healthshield.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRiskScoringService {

    @Value("${anthropic.api.key}")
    private String apiKey;

    private static final String CLAUDE_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL      = "claude-sonnet-4-20250514";

    private final PolicyRepository policyRepo;
    private final ObjectMapper     objectMapper;

    /**
     * Call this right after setExtractedConditions + setAiAnalysisDone(true)
     * in your existing AI analysis service. Runs async so it never blocks.
     */
    @Async
    public void scorePolicy(Policy policy) {
        try {
            String raw = callClaude(buildPrompt(policy));
            applyScore(policy, raw);
            policyRepo.save(policy);
            log.info("Risk scored — policy {} → {}/100 ({})",
                    policy.getPolicyNumber(),
                    policy.getRiskScore(),
                    policy.getRiskLevel());
        } catch (Exception e) {
            log.error("Risk scoring failed for policy {}: {}",
                    policy.getPolicyNumber(), e.getMessage());
        }
    }

    // ── Prompt ─────────────────────────────────────────────────────────────

    private String buildPrompt(Policy policy) {
        String conditions = Optional.ofNullable(policy.getExtractedConditions())
                .filter(c -> !c.isBlank() && !c.equalsIgnoreCase("None"))
                .orElse("None");

        List<String> members = new ArrayList<>();
        if (policy.getMembers() != null) {
            for (PolicyMember m : policy.getMembers()) {
                int age = m.getDateOfBirth() != null
                        ? Period.between(m.getDateOfBirth(), LocalDate.now()).getYears()
                        : 0;
                members.add(m.getRelationship() + ", age " + age
                        + ", " + m.getGender());
            }
        }

        return """
            You are a senior health insurance underwriter AI.
            Assess the risk for this policy and return ONLY valid JSON — no markdown, no explanation.

            Policy details:
            - Extracted medical conditions: %s
            - Members: %s
            - Coverage amount: ₹%s

            Return exactly this structure:
            {
              "score": <integer 0-100>,
              "level": "<LOW|MEDIUM|HIGH|CRITICAL>",
              "summary": "<one sentence for the underwriter, max 20 words>",
              "factors": ["<factor 1>", "<factor 2>"]
            }

            Scoring guide:
            0–25   LOW      — no significant conditions, young healthy members
            26–50  MEDIUM   — minor or well-managed conditions
            51–75  HIGH     — multiple conditions or one severe condition
            76–100 CRITICAL — multiple severe conditions, high claim probability

            Condition weights (add to base score):
            Diabetes +20, Hypertension +15, Heart Disease +25, Cancer +30,
            Kidney Disease +25, Liver Disease +20, Obesity +10, Asthma +10.
            Modifiers: +5 per member over age 55, +10 if 3 or more conditions.
            """.formatted(
                conditions,
                members.isEmpty() ? "Not specified" : String.join(" | ", members),
                policy.getCoverageAmount());
    }

    // ── Claude API ─────────────────────────────────────────────────────────

    private String callClaude(String prompt) throws Exception {
        RestTemplate rest = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
                "model",      MODEL,
                "max_tokens", 512,
                "messages",   List.of(Map.of("role", "user", "content", prompt))
        );

        ResponseEntity<String> resp = rest.postForEntity(
                CLAUDE_URL,
                new HttpEntity<>(body, headers),
                String.class);

        JsonNode root = objectMapper.readTree(resp.getBody());
        return root.path("content").get(0).path("text").asText();
    }

    // ── Apply result ───────────────────────────────────────────────────────

    private void applyScore(Policy policy, String json) throws Exception {
        String clean = json.replaceAll("```json|```", "").trim();
        JsonNode node = objectMapper.readTree(clean);
        policy.setRiskScore(node.path("score").asInt());
        policy.setRiskLevel(node.path("level").asText("MEDIUM"));
        policy.setRiskSummary(node.path("summary").asText());
    }
}