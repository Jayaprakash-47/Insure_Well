package com.healthshield.service;


import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final BaseColor BRAND_BLUE = new BaseColor(30, 64, 175);
    private static final BaseColor LIGHT_BLUE = new BaseColor(239, 246, 255);
    private static final BaseColor GRAY = new BaseColor(107, 114, 128);

    /**
     * Generates a Policy Certificate PDF.
     * Pass in the details you have from your Policy + User entities.
     */
    public byte[] generatePolicyCertificate(
            String policyNumber,
            String holderName,
            String holderEmail,
            String planName,
            String sumInsured,
            String premiumAmount,
            LocalDate startDate,
            LocalDate expiryDate,
            String status) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // ── Header bar ──
            Font titleFont  = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, BaseColor.WHITE);
            Font heading    = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BRAND_BLUE);
            Font normal     = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);
            Font small      = new Font(Font.FontFamily.HELVETICA, 9,  Font.ITALIC, GRAY);

            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);
            PdfPCell headerCell = new PdfPCell(new Phrase("HealthShield Insurance", titleFont));
            headerCell.setBackgroundColor(BRAND_BLUE);
            headerCell.setPadding(16);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setBorder(Rectangle.NO_BORDER);
            header.addCell(headerCell);
            document.add(header);
            document.add(new Paragraph("\n"));

            // ── Certificate title ──
            Paragraph certTitle = new Paragraph("POLICY CERTIFICATE",
                    new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BRAND_BLUE));
            certTitle.setAlignment(Element.ALIGN_CENTER);
            certTitle.setSpacingAfter(16);
            document.add(certTitle);

            // ── Details table ──
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 3f});
            table.setSpacingAfter(20);

            addRow(table, "Policy Number",   policyNumber,  heading, normal);
            addRow(table, "Plan Name",        planName,      heading, normal);
            addRow(table, "Policy Holder",    holderName,    heading, normal);
            addRow(table, "Email",            holderEmail,   heading, normal);
            addRow(table, "Sum Insured",      "₹" + sumInsured,   heading, normal);
            addRow(table, "Premium Amount",   "₹" + premiumAmount, heading, normal);
            addRow(table, "Start Date",       startDate  != null ? startDate.format(FMT)  : "N/A", heading, normal);
            addRow(table, "Expiry Date",      expiryDate != null ? expiryDate.format(FMT) : "N/A", heading, normal);
            addRow(table, "Status",           status,        heading, normal);
            document.add(table);

            // ── Footer ──
            Paragraph footer = new Paragraph(
                    "This is a computer-generated certificate and is valid without a physical signature.\n" +
                            "Generated on: " + LocalDate.now().format(FMT), small);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void addRow(PdfPTable table, String label, String value,
                        Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(LIGHT_BLUE);
        labelCell.setPadding(9);
        labelCell.setBorderColor(new BaseColor(219, 234, 254));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", valueFont));
        valueCell.setPadding(9);
        valueCell.setBorderColor(new BaseColor(219, 234, 254));
        table.addCell(valueCell);
    }
}