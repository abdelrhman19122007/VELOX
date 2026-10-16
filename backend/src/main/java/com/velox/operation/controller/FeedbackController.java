package com.velox.operation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.velox.operation.dto.OperationDtos.ComplaintCreated;
import com.velox.operation.dto.OperationDtos.ComplaintRequest;
import com.velox.operation.dto.OperationDtos.ReviewRequest;
import com.velox.operation.repository.FeedbackRepository.Complaint;
import com.velox.operation.service.FeedbackService;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;

/** The customer's side of feedback: rate a delivered order, complain about an order. */
@RestController
public class FeedbackController {

    private final FeedbackService feedback;

    public FeedbackController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    @PostMapping("/api/orders/{code}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public void review(@CurrentUser AuthUser me, @PathVariable String code, @Valid @RequestBody ReviewRequest req) {
        feedback.review(me, code, req.storeRating(), req.driverRating(), req.comment());
    }

    @PostMapping("/api/orders/{code}/complaint")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintCreated complain(@CurrentUser AuthUser me, @PathVariable String code, @Valid @RequestBody ComplaintRequest req) {
        return feedback.complain(me, code, req.details());
    }

    @GetMapping("/api/me/complaints")
    public List<Complaint> mine(@CurrentUser AuthUser me) {
        return feedback.myComplaints(me);
    }
}
