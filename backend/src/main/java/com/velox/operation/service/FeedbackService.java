package com.velox.operation.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.common.error.ApiException;
import com.velox.operation.dto.OperationDtos.ComplaintCreated;
import com.velox.operation.repository.FeedbackRepository;
import com.velox.operation.repository.FeedbackRepository.Complaint;
import com.velox.order.domain.Order;
import com.velox.order.domain.OrderStatus;
import com.velox.order.repository.OrderRepository;
import com.velox.payment.repository.PromoRepository;
import com.velox.payment.repository.PromoRepository.Promo;
import com.velox.security.AuditLog;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/**
 * Reviews and complaints (FR-OPS-12…14). A customer reviews their own delivered order once; complains about their
 * own order; the admin closes the complaint, optionally with a one-time discount code for that customer only.
 */
@Service
public class FeedbackService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final FeedbackRepository feedback;
    private final OrderRepository orders;
    private final PromoRepository promos;
    private final AuditLog audit;
    private final Clock clock;

    public FeedbackService(FeedbackRepository feedback, OrderRepository orders, PromoRepository promos, AuditLog audit, Clock clock) {
        this.feedback = feedback;
        this.orders = orders;
        this.promos = promos;
        this.audit = audit;
        this.clock = clock;
    }

    public void review(AuthUser me, String code, int storeRating, Integer driverRating, String comment) {
        Order o = own(me, code);
        if (o.status() != OrderStatus.DELIVERED) {
            throw ApiException.conflict("NOT_DELIVERED", "You can rate an order after it arrives");
        }
        String text = comment == null || comment.isBlank() ? null : comment.trim();
        if (!feedback.insertReview(o.id(), storeRating, o.driverId() == null ? null : driverRating, text)) {
            throw ApiException.conflict("ALREADY_REVIEWED", "You already rated this order");
        }
    }

    @Transactional
    public ComplaintCreated complain(AuthUser me, String code, String details) {
        Order o = own(me, code);
        if (o.status() == OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("BAD_STATUS", "This order wasn't placed yet");
        }
        if (feedback.hasOpenComplaint(o.id())) {
            throw ApiException.conflict("COMPLAINT_OPEN", "There's already an open complaint for this order");
        }
        String complaintCode = "CMP-" + random(6);
        feedback.insertComplaint(complaintCode, o.id(), details.trim());
        return new ComplaintCreated(complaintCode);
    }

    /** Anyone may read a store's reviews: they're shown on its page. */
    public List<FeedbackRepository.Review> publicReviews(String storeId) {
        return feedback.storeReviews(storeId);
    }

    public List<Complaint> myComplaints(AuthUser me) {
        me.require(Role.CUSTOMER);
        return feedback.complaintsOf(me.id());
    }

    public List<Complaint> complaints(AuthUser admin, String status) {
        admin.require(Role.ADMIN);
        String s = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        if (s != null && !s.equals("OPEN") && !s.equals("CLOSED")) {
            throw ApiException.badRequest("INVALID_REQUEST", "OPEN or CLOSED");
        }
        return feedback.complaints(s);
    }

    @Transactional
    public Complaint close(AuthUser admin, String code, String note, BigDecimal compensation) {
        admin.require(Role.ADMIN);
        Complaint c = feedback.findComplaint(code == null ? "" : code.trim().toUpperCase())
                .orElseThrow(() -> ApiException.notFound("UNKNOWN_COMPLAINT", "No such complaint"));
        String promo = null;
        if (compensation != null && compensation.signum() > 0) {
            promo = "SORRY" + random(6);
            promos.insert(new Promo(promo, "FIXED", compensation, BigDecimal.ZERO, c.customerId(), 1, 0,
                    clock.instant().plus(Duration.ofDays(30))));
        }
        if (!feedback.close(c.code(), note == null || note.isBlank() ? null : note.trim(), promo)) {
            throw ApiException.conflict("COMPLAINT_CLOSED", "This complaint is already closed");
        }
        audit.record(admin, "COMPLAINT_CLOSE", "complaint", c.code(), promo == null ? null : "compensation " + compensation);
        return feedback.findComplaint(c.code()).orElseThrow();
    }

    private Order own(AuthUser me, String code) {
        me.require(Role.CUSTOMER);
        return orders.findByCode(code == null ? "" : code.trim().toUpperCase())
                .filter(o -> o.customerId() == me.id())
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found"));
    }

    private static String random(int length) {
        StringBuilder b = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            b.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return b.toString();
    }
}
