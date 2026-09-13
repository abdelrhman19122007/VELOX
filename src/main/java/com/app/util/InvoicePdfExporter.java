package com.app.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal PDF exporter for VELOX invoices.
 * Pure JDK only (no external PDF library) using standard Type1 fonts.
 * The receipt text produced by {@link ReceiptGenerator} is Latin-based,
 * so Helvetica/WinAnsi is sufficient. Non-encodable chars become '?'.
 */
public final class InvoicePdfExporter {

    private static final int PAGE_W = 595;   // A4 width (pt)
    private static final int PAGE_H = 842;   // A4 height (pt)
    private static final int MARGIN = 50;
    private static final int TITLE_SIZE = 14;
    private static final int BODY_SIZE = 10;
    private static final int LEADING = 13;
    private static final int BODY_LINES_PER_PAGE = 55;

    private InvoicePdfExporter() {
    }

    /**
     * Exports receipt text to a PDF file, creating parent dirs as needed.
     *
     * @param receiptText plain-text receipt from {@link ReceiptGenerator}
     * @param orderId     used in the PDF title
     * @param target      destination .pdf path
     * @return the target path
     */
    public static Path export(String receiptText, String orderId, Path target) throws IOException {
        if (receiptText == null) {
            receiptText = "";
        }
        if (orderId == null || orderId.isBlank()) {
            orderId = "UNKNOWN";
        }
        List<String> body = sanitizeLines(receiptText);
        List<List<String>> pages = paginate(body);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        write(out, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");

        // Object numbering: 1 catalog, 2 pages, 3 font regular, 4 font bold,
        // then per page: page object + content object.
        int pageCount = pages.size();
        int firstPageObj = 5;
        List<Integer> pageObjs = new ArrayList<>();
        List<Integer> contentObjs = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            pageObjs.add(firstPageObj + i * 2);
            contentObjs.add(firstPageObj + i * 2 + 1);
        }
        int nextObj = firstPageObj + pageCount * 2;

        // 1 catalog
        offsets.add(out.size());
        write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // 2 pages
        StringBuilder kids = new StringBuilder();
        for (int p : pageObjs) {
            kids.append(p).append(" 0 R ");
        }
        offsets.add(out.size());
        write(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>\nendobj\n");

        // 3/4 fonts
        offsets.add(out.size());
        write(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
        offsets.add(out.size());
        write(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        for (int i = 0; i < pageCount; i++) {
            // page object
            offsets.add(out.size());
            write(out, pageObjs.get(i) + " 0 obj\n<< /Type /Page /Parent 2 0 R "
                    + "/MediaBox [0 0 " + PAGE_W + " " + PAGE_H + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> "
                    + "/Contents " + contentObjs.get(i) + " 0 R >>\nendobj\n");

            // content stream (each line uses absolute Tm positioning)
            StringBuilder sb = new StringBuilder();
            float y = PAGE_H - MARGIN;
            if (i == 0) {
                sb.append(textLine("/F2", TITLE_SIZE, MARGIN, y, "VELOX - Official Receipt - " + orderId));
                y -= TITLE_SIZE + 8;
                sb.append(textLine("/F1", 9, MARGIN, y, "Exported: " + stamp + "   |   Page " + (i + 1) + "/" + pageCount));
                y -= LEADING + 4;
            } else {
                sb.append(textLine("/F1", 9, MARGIN, y, "Order " + orderId + " (cont.)   |   Page " + (i + 1) + "/" + pageCount));
                y -= LEADING + 4;
            }
            for (String line : pages.get(i)) {
                sb.append(textLine("/F1", BODY_SIZE, MARGIN, y, line));
                y -= LEADING;
            }
            byte[] stream = sb.toString().getBytes(StandardCharsets.ISO_8859_1);
            offsets.add(out.size());
            write(out, contentObjs.get(i) + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
            out.write(stream);
            write(out, "\nendstream\nendobj\n");
        }

        int xrefPos = out.size();
        write(out, "xref\n0 " + nextObj + "\n");
        write(out, "0000000000 65535 f \n");
        for (int off : offsets) {
            write(out, String.format("%010d 00000 n \n", off));
        }
        write(out, "trailer\n<< /Size " + nextObj + " /Root 1 0 R >>\nstartxref\n" + xrefPos + "\n%%EOF");

        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.write(target, out.toByteArray());
        return target;
    }

    private static List<String> sanitizeLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String raw : text.split("\r?\n")) {
            StringBuilder sb = new StringBuilder(raw.length());
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (c == '\t') {
                    sb.append("    ");
                } else if (c >= 32 && c <= 126) {
                    sb.append(c);
                } else if (c >= 160 && c <= 255) {
                    sb.append(c);
                } else {
                    sb.append('?');
                }
            }
            // Wrap long lines at 95 chars to fit A4 with font 10
            String line = sb.toString();
            while (line.length() > 95) {
                lines.add(line.substring(0, 95));
                line = line.substring(95);
            }
            lines.add(line);
        }
        if (lines.isEmpty()) {
            lines.add("(empty receipt)");
        }
        return lines;
    }

    private static List<List<String>> paginate(List<String> body) {
        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < body.size(); i += BODY_LINES_PER_PAGE) {
            pages.add(new ArrayList<>(body.subList(i, Math.min(i + BODY_LINES_PER_PAGE, body.size()))));
        }
        if (pages.isEmpty()) {
            List<String> one = new ArrayList<>();
            one.add("(empty)");
            pages.add(one);
        }
        return pages;
    }

    private static String textLine(String font, int size, float x, float y, String text) {
        return "BT " + font + " " + size + " Tf 1 0 0 1 " + x + " " + y + " Tm (" + escape(text) + ") Tj ET\n";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static void write(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
