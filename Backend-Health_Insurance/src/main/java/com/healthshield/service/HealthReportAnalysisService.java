package com.healthshield.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;

/**
 * AI-powered health report analysis service.
 *
 * Flow:
 *   1. Read the uploaded health report PDF from disk
 *   2. Extract all text using Apache PDFBox
 *   3. Send the text to Claude API (claude-haiku-4-5)
 *   4. Claude first VALIDATES the document is a genuine health report
 *   5. If suspicious → returns "SUSPICIOUS_DOCUMENT"
 *   6. If valid     → returns comma-separated medical conditions
 *   7. PolicyService.updateExtractedConditions saves result + pushes SSE
 */
@Service
@Slf4j
public class HealthReportAnalysisService {

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    private static final String CLAUDE_API_URL     = "https://api.anthropic.com/v1/messages";
    private static final String CLAUDE_MODEL        = "claude-haiku-4-5-20251001";

    // FIX 3: Sentinel value stored in DB when document is not a health report
    public static final String SUSPICIOUS_DOCUMENT = "SUSPICIOUS_DOCUMENT";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient   httpClient   = HttpClient.newHttpClient();

    public HealthReportAnalysisService() {}

    // ══════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Analyzes a health report PDF.
     *
     * @param pdfFilePath absolute path to the PDF on disk
     * @return "Diabetes, Hypertension" | "SUSPICIOUS_DOCUMENT" | null (no conditions)
     */
    public String analyzeHealthReport(String pdfFilePath) {
        if (pdfFilePath == null || pdfFilePath.isBlank()) {
            log.warn("No PDF path provided for health report analysis");
            return null;
        }
        try {
            String pdfText = extractTextFromPdf(pdfFilePath);

            // FIX 3: Very short text = blank/image-only doc = suspicious
            if (pdfText == null || pdfText.trim().length() < 30) {
                log.warn("PDF text too short — possible image-only or blank doc: {}", pdfFilePath);
                return SUSPICIOUS_DOCUMENT;
            }

            log.info("Extracted {} chars from PDF: {}", pdfText.length(), pdfFilePath);
            String result = callClaudeForConditions(pdfText);
            log.info("AI analysis result for {}: {}", pdfFilePath, result);
            return result;

        } catch (Exception e) {
            log.error("Health report analysis failed for {}: {}", pdfFilePath, e.getMessage());
            return null;
        }
    }

    /**
     * Async entry point — called after policy document upload.
     * Runs in background thread; result is saved + SSE-pushed via PolicyService.
     */
    @Async
    public void analyzeAndUpdatePolicy(
            String pdfFilePath,
            Long policyId,
            PolicyService policyService) {
        try {
            log.info("Starting async AI analysis for policy {}", policyId);
            String result = analyzeHealthReport(pdfFilePath);
            policyService.updateExtractedConditions(policyId, result);
            log.info("Async AI analysis complete for policy {} — result: {}", policyId, result);
        } catch (Exception e) {
            log.error("Async analysis failed for policy {}: {}", policyId, e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 1 — PDF TEXT EXTRACTION
    // ══════════════════════════════════════════════════════════════════════

    private String extractTextFromPdf(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            log.warn("PDF file not found: {}", filePath);
            return null;
        }
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            // Truncate to 3000 chars — enough context for Claude, saves tokens
            if (text.length() > 3000) text = text.substring(0, 3000);
            return text.trim();
        } catch (IOException e) {
            log.error("Failed to extract PDF text: {}", e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 2 — CLAUDE API CALL WITH DOCUMENT VALIDATION
    // ══════════════════════════════════════════════════════════════════════

    private String callClaudeForConditions(String pdfText) throws Exception {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            log.warn("Anthropic API key not configured — using keyword fallback");
            return extractConditionsWithKeywords(pdfText);
        }

        // FIX 3: Two-stage prompt — validate first, then extract
        String prompt = """
                You are a medical document validator for an insurance underwriting system.

                STEP 1 — DOCUMENT VALIDATION:
                First determine if this is a GENUINE medical health report.
                A genuine health report contains at least some of:
                patient name, date, doctor/hospital name, test results, diagnoses,
                vital signs, clinical findings, blood work, or prescriptions.

                These are NOT valid health reports (mark as suspicious):
                - Identity documents (Aadhaar card, PAN card, passport, driving licence)
                - Invoices, bills, receipts without clinical context
                - Resumes, CVs, or academic certificates
                - Legal documents or contracts
                - News articles, promotional material, or random text
                - Completely blank content or gibberish
                - Images that produced no meaningful text

                If the document is NOT a genuine health/medical report:
                Respond with exactly this word only: SUSPICIOUS_DOCUMENT

                STEP 2 — CONDITION EXTRACTION (only if document is genuine):
                Extract ALL medical conditions, diseases, and diagnoses mentioned.

                Rules:
                - Return ONLY a comma-separated list. No explanation, no preamble.
                - Use standard medical names (e.g. "Type 2 Diabetes" not "sugar problem")
                - If no conditions found in a genuine report, return exactly: None
                - Do NOT include medications, symptoms, or lab values — only diagnoses
                - Maximum 10 conditions

                Document Text:
                %s
                """.formatted(pdfText);

        Map<String, Object> requestBody = Map.of(
                "model",      CLAUDE_MODEL,
                "max_tokens", 300,
                "messages",   List.of(Map.of("role", "user", "content", prompt))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CLAUDE_API_URL))
                .header("Content-Type",      "application/json")
                .header("x-api-key",         anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Claude API error {}: {}", response.statusCode(), response.body());
            // API failure — fall back to keyword extraction (don't mark suspicious)
            return extractConditionsWithKeywords(pdfText);
        }

        JsonNode root    = objectMapper.readTree(response.body());
        String   content = root.path("content").path(0).path("text").asText("").trim();

        log.info("Claude raw response: '{}'", content);

        // FIX 3: Pass SUSPICIOUS_DOCUMENT through exactly
        if (SUSPICIOUS_DOCUMENT.equalsIgnoreCase(content.trim())) {
            return SUSPICIOUS_DOCUMENT;
        }

        if (content.isBlank() || content.equalsIgnoreCase("none")) return null;
        return content;
    }

    // ══════════════════════════════════════════════════════════════════════
    // FALLBACK — KEYWORD EXTRACTION (when no API key configured)
    // ══════════════════════════════════════════════════════════════════════

    private String extractConditionsWithKeywords(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();

        // FIX 3: Basic medical marker check — at least one must be present
        List<String> medicalMarkers = List.of(
                "patient", "diagnosis", "hospital", "doctor", "dr.", "physician",
                "blood", "report", "laboratory", "lab", "test", "result",
                "mg/dl", "mmhg", "bmi", "ecg", "hba1c", "cholesterol",
                "prescription", "medicine", "treatment", "clinical", "pathology"
        );

        boolean hasMedicalContent = medicalMarkers.stream().anyMatch(lower::contains);

        if (!hasMedicalContent) {
            log.warn("No medical markers found in document — marking as suspicious");
            return SUSPICIOUS_DOCUMENT;
        }

        // Document looks medical — extract conditions
        Map<String, String> conditionMap = new LinkedHashMap<>();
        conditionMap.put("type 2 diabetes",     "Type 2 Diabetes");
        conditionMap.put("type 1 diabetes",     "Type 1 Diabetes");
        conditionMap.put("diabetes",            "Diabetes");
        conditionMap.put("diabetic",            "Diabetes");
        conditionMap.put("hypertension",        "Hypertension");
        conditionMap.put("high blood pressure", "Hypertension");
        conditionMap.put("heart disease",       "Heart Disease");
        conditionMap.put("cardiac",             "Cardiac Condition");
        conditionMap.put("coronary",            "Coronary Artery Disease");
        conditionMap.put("asthma",              "Asthma");
        conditionMap.put("copd",                "COPD");
        conditionMap.put("obesity",             "Obesity");
        conditionMap.put("overweight",          "Overweight");
        conditionMap.put("hypothyroid",         "Hypothyroidism");
        conditionMap.put("hyperthyroid",        "Hyperthyroidism");
        conditionMap.put("thyroid",             "Thyroid Disorder");
        conditionMap.put("hyperlipidemia",      "Hyperlipidemia");
        conditionMap.put("dyslipidaemia",       "Dyslipidaemia");
        conditionMap.put("cholesterol",         "High Cholesterol");
        conditionMap.put("arthritis",           "Arthritis");
        conditionMap.put("cancer",              "Cancer");
        conditionMap.put("tumor",               "Tumor");
        conditionMap.put("kidney disease",      "Kidney Disease");
        conditionMap.put("renal",               "Renal Condition");
        conditionMap.put("fatty liver",         "Fatty Liver");
        conditionMap.put("liver disease",       "Liver Disease");
        conditionMap.put("cirrhosis",           "Cirrhosis");
        conditionMap.put("stroke",              "Stroke");
        conditionMap.put("epilepsy",            "Epilepsy");
        conditionMap.put("seizure",             "Seizures");
        conditionMap.put("depression",          "Depression");
        conditionMap.put("anxiety",             "Anxiety Disorder");
        conditionMap.put("anemia",              "Anemia");
        conditionMap.put("osteoporosis",        "Osteoporosis");
        conditionMap.put("psoriasis",           "Psoriasis");
        conditionMap.put("eczema",              "Eczema");
        conditionMap.put("migraine",            "Migraine");
        conditionMap.put("gout",                "Gout");
        conditionMap.put("ulcer",               "Peptic Ulcer");
        conditionMap.put("gastric",             "Gastric Condition");
        conditionMap.put("ibs",                 "Irritable Bowel Syndrome");
        conditionMap.put("crohn",               "Crohn's Disease");
        conditionMap.put("hyperuricaemia",      "Hyperuricaemia");
        conditionMap.put("ckd",                 "Chronic Kidney Disease");

        List<String> found    = new ArrayList<>();
        Set<String>  addedSet = new HashSet<>();

        for (Map.Entry<String, String> entry : conditionMap.entrySet()) {
            if (lower.contains(entry.getKey()) && addedSet.add(entry.getValue())) {
                found.add(entry.getValue());
            }
        }

        return found.isEmpty() ? null : String.join(", ", found);
    }
}