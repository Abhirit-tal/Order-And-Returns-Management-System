package com.example.ordermanagement.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PdfService {

    @Value("${app.invoices.path:${java.io.tmpdir}/invoices}")
    private String invoicesPath;

    public byte[] generateInvoicePdf(UUID orderId, String customerEmail) throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
            cs.newLineAtOffset(50, 700);
            cs.showText("ArtiCurated - Invoice");
            cs.endText();

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 12);
            cs.newLineAtOffset(50, 660);
            cs.showText("Order: " + orderId.toString());
            cs.newLineAtOffset(0, -15);
            cs.showText("Customer: " + customerEmail);
            cs.newLineAtOffset(0, -15);
            cs.showText("Date: " + OffsetDateTime.now().toString());
            cs.endText();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();

        // persist to filesystem for now
        File dir = new File(invoicesPath);
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, orderId + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(baos.toByteArray());
        }

        return baos.toByteArray();
    }
}

