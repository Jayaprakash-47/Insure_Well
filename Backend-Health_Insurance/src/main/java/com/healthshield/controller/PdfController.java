package com.healthshield.controller;
import com.healthshield.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://localhost:62486"
})
public class PdfController {

    private final PdfService pdfService;

    /**
     * Call this from your existing policy details page.
     * Wire in your PolicyRepository + Policy entity fields here.
     *
     * Example URL: GET /api/pdf/policy/certificate?policyNumber=POL-001&...
     *
     * Replace the @RequestParam fields with a proper @PathVariable policyId
     * and fetch from your PolicyRepository once you share that entity.
     */
    @GetMapping("/policy/certificate")
    public ResponseEntity<byte[]> downloadPolicyCertificate(
            @RequestParam String policyNumber,
            @RequestParam String planName,
            @RequestParam String sumInsured,
            @RequestParam String premiumAmount,
            @RequestParam String startDate,
            @RequestParam String expiryDate,
            @RequestParam String status,
            Authentication auth) {

        // auth.getName() = logged-in user's email
        String email = auth.getName();

        byte[] pdf = pdfService.generatePolicyCertificate(
                policyNumber,
                email,           // or fetch name from UserRepository
                email,
                planName,
                sumInsured,
                premiumAmount,
                LocalDate.parse(startDate),
                LocalDate.parse(expiryDate),
                status
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "policy_" + policyNumber + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}