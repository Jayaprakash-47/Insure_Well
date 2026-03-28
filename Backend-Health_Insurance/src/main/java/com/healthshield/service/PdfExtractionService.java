package com.healthshield.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PdfExtractionService {

    /**
     * Extracts the highest currency amount found in the given PDF file.
     * Assumes the highest amount is the total bill amount.
     */
    public BigDecimal extractHighestAmount(java.nio.file.Path filePath) {
        if (filePath == null || !java.nio.file.Files.exists(filePath)) return null;

        try (InputStream is = java.nio.file.Files.newInputStream(filePath);
             PDDocument document = Loader.loadPDF(is.readAllBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            return findHighestAmount(text);

        } catch (Exception e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage());
            return null;
        }
    }

    private BigDecimal findHighestAmount(String text) {
        if (text == null || text.isBlank()) return null;

        BigDecimal highest = BigDecimal.ZERO;
        boolean found = false;

        // Match typical Indian currency formats: 
        // ₹ 50000, Rs. 50,000, INR 50000.00, or just lines with "Total: 50000"
        // This regex looks for:
        // (₹|Rs\.?|INR|Total[:\-]?)\s*([\d,]+(\.\d{1,2})?)
        String regex = "(?:₹|Rs\\.?|INR|Total|Amount)[:\\-]?\\s*([\\d,]+(?:\\.\\d{1,2})?)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            try {
                // The amount string is in group 1. Remove commas.
                String amountStr = matcher.group(1).replace(",", "");
                BigDecimal amount = new BigDecimal(amountStr);

                if (amount.compareTo(highest) > 0) {
                    highest = amount;
                    found = true;
                }
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
            }
        }

        // If no specific currency marker was found, fallback to scanning ALL numbers
        // and picking the highest reasonable amount (to catch plain tables).
        if (!found) {
            Pattern fallbackPattern = Pattern.compile("\\b([1-9][\\d,]*(\\.\\d{1,2})?)\\b");
            Matcher fallbackMatcher = fallbackPattern.matcher(text);
            
            while (fallbackMatcher.find()) {
                try {
                    String amountStr = fallbackMatcher.group(1).replace(",", "");
                    BigDecimal amount = new BigDecimal(amountStr);
                    
                    // Filter out years (like 2024), phone numbers, etc. 
                    // Assume hospital bills are generally over 100 for it to be the "Total".
                    if (amount.compareTo(highest) > 0 && amount.compareTo(new BigDecimal("100")) > 0) {
                        highest = amount;
                        found = true;
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        return found ? highest : null;
    }
}
