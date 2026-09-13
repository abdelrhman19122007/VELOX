/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.model.order;

/**
 *
 * @author 3bdelr7man
**/
import com.app.enums.ComplaintStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// كلاس يحتوي على جميع تفاصيل الشكوى
public class Complaint implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String complaintId;
    private final String orderId;
    private final String details;
    private ComplaintStatus status;
    private final LocalDateTime createdAt;

    public Complaint(String complaintId, String orderId, String details) {
        if (complaintId == null || complaintId.trim().isEmpty()) {
            throw new IllegalArgumentException("Complaint ID is required");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        this.complaintId = complaintId.trim();
        this.orderId = orderId.trim();
        this.details = details == null ? "" : details.trim();
        this.status = ComplaintStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public String getComplaintId() { return complaintId; }
    public String getOrderId() { return orderId; }
    public String getDetails() { return details; }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String toFileString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return complaintId + " | Order: " + orderId + " | Status: " + status.getDescription() + 
               " | Date: " + createdAt.format(fmt) + " | Details: " + details;
    }
}