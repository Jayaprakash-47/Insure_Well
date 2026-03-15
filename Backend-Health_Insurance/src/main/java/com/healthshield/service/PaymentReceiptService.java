package com.healthshield.service;

import com.healthshield.entity.Payment;
import com.healthshield.repository.PaymentRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PaymentReceiptService {

    private final PaymentRepository paymentRepository;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final BaseColor BRAND   = new BaseColor(30, 64, 175);
    private static final BaseColor LIGHT   = new BaseColor(239, 246, 255);
    private static final BaseColor SUCCESS = new BaseColor(22, 163, 74);

    public byte[] generateReceipt(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont  = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,
                    BaseColor.WHITE);
            Font heading    = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,
                    BRAND);
            Font normal     = new Font(Font.FontFamily.HELVETICA, 11,
                    Font.NORMAL, BaseColor.BLACK);
            Font small      = new Font(Font.FontFamily.HELVETICA, 9,
                    Font.ITALIC, BaseColor.GRAY);
            Font successFont = new Font(Font.FontFamily.HELVETICA, 16,
                    Font.BOLD, SUCCESS);

            // ── Header ──
            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);
            PdfPCell hCell = new PdfPCell(
                    new Phrase("InsureWell Health Insurance", titleFont));
            hCell.setBackgroundColor(BRAND);
            hCell.setPadding(16);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            hCell.setBorder(Rectangle.NO_BORDER);
            header.addCell(hCell);
            doc.add(header);
            doc.add(new Paragraph("\n"));

            // ── Payment Success Badge ──
            Paragraph badge = new Paragraph("✓  PAYMENT SUCCESSFUL", successFont);
            badge.setAlignment(Element.ALIGN_CENTER);
            badge.setSpacingAfter(4);
            doc.add(badge);

            Paragraph sub = new Paragraph("Official Payment Receipt", small);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(16);
            doc.add(sub);

            // ── Details Table ──
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 3f});
            table.setSpacingAfter(20);

            addRow(table, "Receipt No",
                    "RCP-" + payment.getPaymentId(), heading, normal);
            addRow(table, "Transaction ID",
                    payment.getTransactionId(), heading, normal);
            addRow(table, "Policy Number",
                    payment.getPolicy().getPolicyNumber(), heading, normal);
            addRow(table, "Plan Name",
                    payment.getPolicy().getPlan().getPlanName(), heading, normal);
            addRow(table, "Customer Name",
                    payment.getUser().getFirstName()
                            + " " + payment.getUser().getLastName(),
                    heading, normal);
            addRow(table, "Email",
                    payment.getUser().getEmail(), heading, normal);
            addRow(table, "Amount Paid",
                    "₹" + payment.getAmount().toPlainString(), heading, normal);
            addRow(table, "Payment Method",
                    payment.getPaymentMethod().name(), heading, normal);
            addRow(table, "Payment Date",
                    payment.getPaymentDate() != null
                            ? payment.getPaymentDate().format(FMT) : "N/A",
                    heading, normal);
            addRow(table, "Status",
                    payment.getPaymentStatus().name(), heading, normal);
            doc.add(table);

            // ── Footer ──
            Paragraph footer = new Paragraph(
                    "This is a computer-generated receipt and is valid without signature.\n"
                            + "For support: support@insurewell.com",
                    small);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Receipt generation failed: "
                    + e.getMessage(), e);
        }
    }

    private void addRow(PdfPTable table, String label, String value,
                        Font lf, Font vf) {
        PdfPCell lc = new PdfPCell(new Phrase(label, lf));
        lc.setBackgroundColor(LIGHT);
        lc.setPadding(9);
        lc.setBorderColor(new BaseColor(219, 234, 254));
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(
                new Phrase(value != null ? value : "N/A", vf));
        vc.setPadding(9);
        vc.setBorderColor(new BaseColor(219, 234, 254));
        table.addCell(vc);
    }
}