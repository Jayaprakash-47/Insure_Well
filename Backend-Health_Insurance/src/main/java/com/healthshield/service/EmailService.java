package com.healthshield.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final N8nEmailService n8nEmailService;

    // @Value("${spring.mail.username}")
    // private String fromEmail;

    @Async
    public void sendStatusChangeEmail(String toEmail, String recipientName,
                                      String policyNumber, String newStatus) {
        try {
            String subject = "Policy Status Update — " + policyNumber;
            String body = buildStatusHtml(recipientName, policyNumber, newStatus);
            n8nEmailService.sendEmail(toEmail, subject, body);
            log.info("Status email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send status email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendRenewalReminderEmail(String toEmail, String recipientName,
                                         String policyNumber, LocalDate expiryDate) {
        try {
            String subject = "Policy Renewal Reminder — " + policyNumber;
            String body = buildRenewalHtml(recipientName, policyNumber, expiryDate);
            n8nEmailService.sendEmail(toEmail, subject, body);
            log.info("Renewal reminder sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send renewal email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            String subject = "Password Reset OTP — HealthShield";
            String body = buildOtpHtml(otp);
            n8nEmailService.sendEmail(toEmail, subject, body);
            log.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ── HTML templates ──

    private String buildStatusHtml(String name, String policyNo, String status) {
        // ── Friendly label map ──
        String friendlyStatus = switch (status.toUpperCase()) {
            case "QUOTE_SENT"      -> "Quote Ready — Please Pay Now";
            case "ACTIVE"          -> "Active ✓";
            case "EXPIRED"         -> "Expired";
            case "CANCELLED"       -> "Cancelled";
            case "CONCERN_RAISED"  -> "Action Required";
            case "APPROVED"        -> "Claim Approved ✓";
            case "PARTIALLY_APPROVED" -> "Partially Approved";
            case "REJECTED"        -> "Rejected";
            case "SETTLED"         -> "Settled — Payment Processed ✓";
            default                -> status;
        };

        String color = switch (status.toUpperCase()) {
            case "ACTIVE", "APPROVED", "SETTLED" -> "#16a34a";
            case "REJECTED", "EXPIRED",
                 "CANCELLED"                     -> "#dc2626";
            case "CONCERN_RAISED"                -> "#f59e0b";
            default                              -> "#d97706";
        };

        return """
        <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
                    border:1px solid #e5e7eb;border-radius:8px;overflow:hidden">
          <div style="background:#1e40af;padding:24px;text-align:center">
            <h2 style="color:white;margin:0">InsureWell Insurance</h2>
          </div>
          <div style="padding:32px">
            <p>Dear <strong>%s</strong>,</p>
            <p>Your policy <strong>%s</strong> status has been updated to:</p>
            <div style="text-align:center;margin:24px 0">
              <span style="background:%s;color:white;padding:12px 28px;
                           border-radius:20px;font-size:16px;font-weight:bold;
                           display:inline-block">
                %s
              </span>
            </div>
            <p>Please log in to your portal for full details.</p>
            <div style="text-align:center;margin-top:24px">
              <a href="http://localhost:4200"
                 style="background:#1e40af;color:white;padding:12px 32px;
                        border-radius:6px;text-decoration:none;font-weight:bold">
                View Portal
              </a>
            </div>
          </div>
          <div style="background:#f9fafb;padding:12px;text-align:center;
                      font-size:12px;color:#6b7280">
            InsureWell Insurance — Automated Notification
          </div>
        </div>
        """.formatted(name, policyNo, color, friendlyStatus);
    }

    private String buildRenewalHtml(String name, String policyNo, LocalDate expiry) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e5e7eb;border-radius:8px;overflow:hidden">
              <div style="background:#1e40af;padding:24px;text-align:center">
                <h2 style="color:white;margin:0">HealthShield Insurance</h2>
              </div>
              <div style="padding:32px">
                <p>Dear <strong>%s</strong>,</p>
                <p>Your policy <strong>%s</strong> is due for renewal on <strong>%s</strong>.</p>
                <p>To ensure uninterrupted coverage, please renew before the expiry date.</p>
                <div style="text-align:center;margin-top:24px">
                  <a href="http://localhost:4200/customer/policies" style="background:#1e40af;color:white;padding:12px 32px;border-radius:6px;text-decoration:none;font-weight:bold">
                    Renew Now
                  </a>
                </div>
              </div>
              <div style="background:#f9fafb;padding:12px;text-align:center;font-size:12px;color:#6b7280">
                HealthShield Insurance — Automated Notification
              </div>
            </div>
            """.formatted(name, policyNo, expiry.toString());
    }

    private String buildOtpHtml(String otp) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e5e7eb;border-radius:8px;overflow:hidden">
              <div style="background:#1e40af;padding:24px;text-align:center">
                <h2 style="color:white;margin:0">HealthShield Insurance</h2>
              </div>
              <div style="padding:32px;text-align:center">
                <p style="font-size:16px">Your password reset OTP is:</p>
                <div style="font-size:40px;font-weight:bold;letter-spacing:14px;color:#1e40af;margin:24px 0;padding:16px;background:#eff6ff;border-radius:8px">
                  %s
                </div>
                <p style="color:#6b7280;font-size:14px">This OTP expires in <strong>15 minutes</strong>.<br>If you did not request this, please ignore this email.</p>
              </div>
              <div style="background:#f9fafb;padding:12px;text-align:center;font-size:12px;color:#6b7280">
                HealthShield Insurance — Automated Notification
              </div>
            </div>
            """.formatted(otp);
    }
//    @Async
    public void sendPolicyActivationEmail(String toEmail, String customerName,
                                          String policyNumber, String planName,
                                          BigDecimal premiumAmount, BigDecimal coverageAmount,
                                          LocalDate startDate, LocalDate endDate) {
        try {
            String subject = "🎉 Welcome to InsureWell — Policy " + policyNumber + " is Active!";
            String body = buildActivationHtml(customerName, policyNumber, planName,
                    premiumAmount, coverageAmount, startDate, endDate);
            n8nEmailService.sendEmail(toEmail, subject, body);
            log.info("Activation email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send activation email: {}", e.getMessage());
        }
    }

    private String buildActivationHtml(String name, String policyNo, String planName,
                                       BigDecimal premium, BigDecimal coverage,
                                       LocalDate start, LocalDate end) {
        return """
        <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
                    border:1px solid #e5e7eb;border-radius:8px;overflow:hidden">
          <div style="background:#1e40af;padding:28px;text-align:center">
            <h2 style="color:white;margin:0;font-size:24px">Policy Activated!</h2>
            <p style="color:#bfdbfe;margin:8px 0 0">InsureWell Health Insurance</p>
          </div>
          <div style="padding:32px">
            <p style="font-size:16px">Dear <strong>%s</strong>,</p>
            <p>Thank you for choosing InsureWell! Your policy is now
               <strong style="color:#16a34a">ACTIVE</strong>.</p>
            <div style="background:#f0fdf4;border:1px solid #bbf7d0;
                        border-radius:8px;padding:20px;margin:20px 0">
              <table style="width:100%%;border-collapse:collapse">
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:14px">
                    Policy Number
                  </td>
                  <td style="padding:8px 0;font-weight:bold;text-align:right">
                    %s
                  </td>
                </tr>
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:14px">
                    Plan
                  </td>
                  <td style="padding:8px 0;font-weight:bold;text-align:right">
                    %s
                  </td>
                </tr>
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:14px">
                    Coverage Amount
                  </td>
                  <td style="padding:8px 0;font-weight:bold;
                             text-align:right;color:#1e40af">
                    \u20B9%s
                  </td>
                </tr>
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:14px">
                    Annual Premium
                  </td>
                  <td style="padding:8px 0;font-weight:bold;text-align:right">
                    \u20B9%s
                  </td>
                </tr>
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:14px">
                    Valid From
                  </td>
                  <td style="padding:8px 0;font-weight:bold;text-align:right">
                    %s
                  </td>
                </tr>
                <tr>
                  <td style="padding:8px 0;color:#6b7280;font-size:14px">
                    Valid Until
                  </td>
                  <td style="padding:8px 0;font-weight:bold;text-align:right">
                    %s
                  </td>
                </tr>
              </table>
            </div>
            <div style="text-align:center;margin-top:24px">
              <a href="http://localhost:4200/customer/policies"
                 style="background:#1e40af;color:white;padding:12px 32px;
                        border-radius:6px;text-decoration:none;font-weight:bold">
                View My Policy
              </a>
            </div>
          </div>
          <div style="background:#f9fafb;padding:12px;text-align:center;
                      font-size:12px;color:#6b7280">
            InsureWell Insurance — Protecting what matters most
          </div>
        </div>
        """.formatted(name, policyNo, planName,
                coverage.toPlainString(),
                premium.toPlainString(),
                start.toString(),
                end.toString());
    }
}