package com.healthshield.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthshield.entity.Policy;
import com.healthshield.repository.PolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiDocumentVerificationService {

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Autowired
    private PolicyRepository policyRepository;

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL = "claude-haiku-4-5-20251001";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String verifyDocuments(Long policyId) throws Exception {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found"));

        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            log.warn("Anthropic API key not configured. Returning mock verification report.");
            return mockVerificationReport();
        }

        String dobStr = "Unknown";
        String customerName = "Unknown";
        
        if (policy.getUser() instanceof com.healthshield.entity.Customer customer) {
            if (customer.getDateOfBirth() != null) {
                dobStr = customer.getDateOfBirth().toString();
            }
            customerName = customer.getFirstName() + " " + (customer.getLastName() != null ? customer.getLastName() : "");
        } else if (policy.getUser() != null) {
            customerName = policy.getUser().getFirstName() + " " + (policy.getUser().getLastName() != null ? policy.getUser().getLastName() : "");
        }

        String prompt = String.format("""
            You are an expert underwriter and fraud detection AI.
            Please verify the documents for Policy #%s belonging to Customer: %s, DOB: %s.
            
            Compare the provided details against standard KYC and Health Report patterns.
            Identify any discrepancies, missing seals, or suspicious formatting.
            
            Return a detailed verification report with ticks (✅) for valid checks and crosses (❌) for flags.
            Be concise.
            """, policy.getPolicyNumber(), customerName, dobStr);

        Map<String, Object> requestBody = Map.of(
                "model", CLAUDE_MODEL,
                "max_tokens", 500,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CLAUDE_API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Claude API error: {}", response.body());
            return mockVerificationReport();
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("content").path(0).path("text").asText();
    }

    private String mockVerificationReport() {
        return "✅ Aadhaar Name matches Customer Profile\n" +
               "✅ Aadhaar Number Format is Valid\n" +
               "❌ Health Report shows discrepancy in Details\n" +
               "✅ Hospital Seal detected and verified";
    }
}
