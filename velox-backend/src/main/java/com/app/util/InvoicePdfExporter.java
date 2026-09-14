package com.app.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * VELOX invoice PDFs with Arabic support (embedded Amiri font, OpenPDF).
 * Same static API as before, so console callers are untouched.
 */
public final class InvoicePdfExporter {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private InvoicePdfExporter() {
    }

    /**
     * Exports plain receipt text (console flow).
     */
    public static Path export(String receiptText, String orderId, Path target) throws Exception {
        if (receiptText == null) {
            receiptText = "";
        }
        if (orderId == null || orderId.isBlank()) {
            orderId = "UNKNOWN";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, out);
        doc.open();
        Font title = font(15, Font.BOLD);
        Font body = font(10, Font.NORMAL);
        addLine(doc, "VELOX - Official Receipt - " + orderId, title);
        addLine(doc, "Exported: " + LocalDateTime.now().format(STAMP), body);
        addLine(doc, " ", body);
        for (String raw : receiptText.split("\r?\n")) {
            addLine(doc, raw, body);
        }
        doc.close();
        return write(target, out);
    }

    /**
     * Exports a database invoice (header + lines from WebOrderDAO.invoiceData).
     */
    @SuppressWarnings("unchecked")
    public static Path exportInvoice(Map<String, Object> header, Path target) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(doc, out);
        doc.open();
        Font title = font(16, Font.BOLD);
        Font head = font(11, Font.NORMAL);
        Font cell = font(10, Font.NORMAL);
        Font cellBold = font(10, Font.BOLD);

        addLine(doc, "VELOX - Official Invoice", title);
        addLine(doc, "Order: " + header.getOrDefault("orderCode", "")
                + "  |  Status: " + header.getOrDefault("status", ""), head);
        addLine(doc, "Date: " + header.getOrDefault("orderDate", ""), head);
        addLine(doc, "Customer: " + header.getOrDefault("customer", "")
                + "  |  " + header.getOrDefault("email", "")
                + "  |  " + header.getOrDefault("phone", ""), head);
        addLine(doc, "Shipping: " + header.getOrDefault("shipping", ""), head);
        addLine(doc, " ", head);

        PdfPTable table = new PdfPTable(new float[]{4, 1, 2, 2});
        table.setWidthPercentage(100);
        table.addCell(cell("Item", cellBold));
        table.addCell(cell("Qty", cellBold));
        table.addCell(cell("Unit (EGP)", cellBold));
        table.addCell(cell("Subtotal (EGP)", cellBold));
        Object lines = header.get("lines");
        if (lines instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> line) {
                    table.addCell(cell(String.valueOf(line.get("name")), cell));
                    table.addCell(cell(String.valueOf(line.get("quantity")), cell));
                    table.addCell(cell(String.valueOf(line.get("unit")), cell));
                    table.addCell(cell(String.valueOf(line.get("subtotal")), cell));
                }
            }
        }
        doc.add(table);
        addLine(doc, " ", head);
        addLine(doc, "Subtotal: " + header.getOrDefault("subtotal", "") + " EGP", head);
        addLine(doc, "Discount: -" + header.getOrDefault("discount", "") + " EGP", head);
        addLine(doc, "Delivery: " + header.getOrDefault("deliveryFee", "") + " EGP", head);
        addLine(doc, "TOTAL: " + header.getOrDefault("total", "") + " EGP", cellBold);
        addLine(doc, " ", head);
        addLine(doc, "Products can be returned within 14 days with this receipt.", head);
        doc.close();
        return write(target, out);
    }

    // ---- internals ----

    private static void addLine(Document doc, String text, Font font) {
        if (text == null) {
            text = "";
        }
        if (containsArabic(text)) {
            PdfPTable t = new PdfPTable(1);
            t.setWidthPercentage(100);
            PdfPCell c = new PdfPCell(new Phrase(text, font));
            c.setBorder(PdfPCell.NO_BORDER);
            c.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            t.addCell(c);
            doc.add(t);
        } else {
            doc.add(new Paragraph(text, font));
        }
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        if (containsArabic(c.getPhrase().getContent())) {
            c.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }
        return c;
    }

    private static boolean containsArabic(String s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0600 && c <= 0x06FF) {
                return true;
            }
        }
        return false;
    }

    private static Font font(int size, int style) throws Exception {
        BaseFont base = baseFont();
        return new Font(base, size, style);
    }

    private static BaseFont baseFont() throws Exception {
        try (InputStream in = InvoicePdfExporter.class.getResourceAsStream("/fonts/Amiri-Regular.ttf")) {
            if (in == null) {
                throw new IllegalStateException("Amiri font not found on classpath");
            }
            byte[] ttf = in.readAllBytes();
            return BaseFont.createFont("Amiri-Regular.ttf", BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, true, ttf, null);
        }
    }

    private static Path write(Path target, ByteArrayOutputStream out) throws Exception {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.write(target, out.toByteArray());
        return target;
    }
}
