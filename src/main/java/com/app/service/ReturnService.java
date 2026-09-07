/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.service;

import com.app.enums.OrderStatus;
import com.app.exception.ReturnPolicyException;
import com.app.model.order.Order;
import com.app.util.Response;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 *
 * @author 3bdelr7man
 */
// طباعة وتأكيد تغليف كافة المنتجات داخل الطلب
public class ReturnService {
    private static final int ALLOWED_RETURN_DAYS = 1;

    // التحقق من سياسة الإرجاع وحساب المبلغ المسترد
    public Response<Double> processReturn(Order order) throws ReturnPolicyException {

        if (order.isReturned() || order.getStatus() == OrderStatus.RETURNED) {
            throw new ReturnPolicyException("This order has already been returned!");
        }

        long daysBetween = ChronoUnit.DAYS.between(order.getOrderDate(), LocalDateTime.now());
        if (daysBetween > ALLOWED_RETURN_DAYS) {
            throw new ReturnPolicyException("Return period expired! Returns are allowed only within "
                    + ALLOWED_RETURN_DAYS + " days.");
        }

        double refundAmount = order.calculateFinalTotal();
        order.setReturned(true);
        order.setStatus(OrderStatus.RETURNED);

        // 4. إرجاع النتيجة مغلفة في كلاس Response
        return new Response<>(refundAmount, "Return processed successfully. Amount refunded: " + refundAmount + " EGP");
    }
}