package com.healthshield;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.junit.jupiter.api.Test;
import java.io.FileOutputStream;

public class GenerateBillPdfTest {

    @Test
    public void generatePdf() throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream("dummy_hospital_bill.pdf"));
        document.open();
        document.add(new Paragraph("Apollo Hospitals - Final Invoice"));
        document.add(new Paragraph("===================================="));
        document.add(new Paragraph("Patient Name: Jane Doe"));
        document.add(new Paragraph("Admission Date: 2026-03-20"));
        document.add(new Paragraph("Discharge Date: 2026-03-24\n\n"));
        document.add(new Paragraph("Charges Breakdown:"));
        document.add(new Paragraph("- Room Rent (4 days):       Rs. 4000"));
        document.add(new Paragraph("- ICU Charges (1 day):      Rs. 5000"));
        document.add(new Paragraph("- Pharmacy / Medicines:     Rs. 2500"));
        document.add(new Paragraph("- Doctor Consultation:      Rs. 3500\n"));
        document.add(new Paragraph("===================================="));
        document.add(new Paragraph("Total Amount: Rs. 15000.00"));
        document.add(new Paragraph("===================================="));
        document.close();
        System.out.println("PDF created successfully!");
    }
}
