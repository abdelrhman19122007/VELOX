package com.velox.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.velox.payment.dto.PaymentDtos.ConfirmRequest;
import com.velox.payment.dto.PaymentDtos.PromoRequest;
import com.velox.payment.dto.PaymentDtos.TopUpRequest;
import com.velox.payment.repository.PromoRepository.Promo;
import com.velox.payment.service.PaymentService;
import com.velox.payment.service.PaymentService.PaymentInfo;
import com.velox.payment.service.PromoAdminService;
import com.velox.payment.service.WalletService;
import com.velox.payment.service.WalletService.WalletView;
import com.velox.security.AuthUser;
import com.velox.security.CurrentUser;

import jakarta.validation.Valid;

/** Online payment of an order, the wallet, and the admin's promo codes. */
@RestController
public class PaymentController {

    private final PaymentService payments;
    private final WalletService wallet;
    private final PromoAdminService promos;

    public PaymentController(PaymentService payments, WalletService wallet, PromoAdminService promos) {
        this.payments = payments;
        this.wallet = wallet;
        this.promos = promos;
    }

    @GetMapping("/api/orders/{code}/payment")
    public PaymentInfo payment(@CurrentUser AuthUser me, @PathVariable String code) {
        return payments.info(me, code);
    }

    @PostMapping("/api/orders/{code}/payment/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@CurrentUser AuthUser me, @PathVariable String code, @Valid @RequestBody ConfirmRequest req) {
        payments.confirmWallet(me, code, req.code());
    }

    @PostMapping("/api/orders/{code}/payment/sandbox-pay")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sandboxPay(@CurrentUser AuthUser me, @PathVariable String code) {
        payments.sandboxPay(me, code);
    }

    @GetMapping("/api/me/wallet")
    public WalletView wallet(@CurrentUser AuthUser me) {
        return wallet.view(me);
    }

    @PostMapping("/api/me/wallet/sandbox-top-up")
    public WalletView topUp(@CurrentUser AuthUser me, @Valid @RequestBody TopUpRequest req) {
        return wallet.sandboxTopUp(me, req.amount());
    }

    @GetMapping("/api/admin/promos")
    public List<Promo> promos(@CurrentUser AuthUser me) {
        return promos.list(me);
    }

    @PostMapping("/api/admin/promos")
    @ResponseStatus(HttpStatus.CREATED)
    public void createPromo(@CurrentUser AuthUser me, @Valid @RequestBody PromoRequest req) {
        promos.create(me, req.toPromo());
    }
}
