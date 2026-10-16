package com.velox.operation.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.velox.operation.dto.OperationDtos.AdminStats;
import com.velox.operation.dto.OperationDtos.CloseComplaintRequest;
import com.velox.operation.dto.OperationDtos.ReasonRequest;
import com.velox.operation.repository.FeedbackRepository.Complaint;
import com.velox.operation.service.AdminOperationsService;
import com.velox.operation.service.FeedbackService;
import com.velox.order.dto.OrderDtos.OrderView;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;

/** The admin dashboard: orders, numbers and complaints. */
@RestController
@RequestMapping("/api/admin")
public class AdminOperationsController {

    private final AdminOperationsService admin;
    private final FeedbackService feedback;

    public AdminOperationsController(AdminOperationsService admin, FeedbackService feedback) {
        this.admin = admin;
        this.feedback = feedback;
    }

    @GetMapping("/orders")
    public List<OrderView> orders(@CurrentUser AuthUser me, @RequestParam(required = false) String status) {
        return admin.orders(me, status);
    }

    @PostMapping("/orders/{code}/cancel")
    public OrderView cancel(@CurrentUser AuthUser me, @PathVariable String code, @Valid @RequestBody ReasonRequest req) {
        return admin.cancel(me, code, req.reason());
    }

    @GetMapping("/stats")
    public AdminStats stats(@CurrentUser AuthUser me) {
        return admin.stats(me);
    }

    @GetMapping("/complaints")
    public List<Complaint> complaints(@CurrentUser AuthUser me, @RequestParam(required = false) String status) {
        return feedback.complaints(me, status);
    }

    @PostMapping("/complaints/{code}/close")
    public Complaint close(@CurrentUser AuthUser me, @PathVariable String code, @Valid @RequestBody CloseComplaintRequest req) {
        return feedback.close(me, code, req.note(), req.compensation());
    }
}
