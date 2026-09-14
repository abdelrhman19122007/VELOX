package com.app.controller;

import com.app.dao.WebOrderDAO;
import com.app.service.AuthTokenStore;
import com.app.service.OrderService;
import com.app.util.InvoicePdfExporter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Invoice download, generated from MySQL rows (owner only).
 *
 * GET /api/orders/{id}/invoice -> application/pdf
 */
@RestController
public class InvoiceController {

    private final WebOrderDAO dao = new WebOrderDAO();
    private final OrderService orders = new OrderService();

    @GetMapping("/api/orders/{id}/invoice")
    public ResponseEntity<?> invoice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") String id) {
        String email = AuthTokenStore.resolve(authorization);
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Login required."));
        }
        Map<String, Object> data;
        try {
            orders.getTracking(id);
            if (!orders.ownsOrder(id, email)) {
                return ResponseEntity.status(403).body(Map.of("message", "Forbidden."));
            }
            data = dao.invoiceData(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path tmp = Files.createTempFile("velox-inv-", ".pdf");
            InvoicePdfExporter.exportInvoice(data, tmp);
            byte[] pdf = Files.readAllBytes(tmp);
            Files.deleteIfExists(tmp);
            String code = String.valueOf(data.getOrDefault("orderCode", id));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("INV-" + code + ".pdf", StandardCharsets.UTF_8).build());
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Invoice failed: " + e.getMessage()));
        }
    }
}
