/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.service;

/**
 *
 * @author 3bdelr7man
 */
import com.app.enums.Zone;

// كلاس حساب تكلفة توصيل الأجهزة الإلكترونية
public class TechDelivery extends Delivery {
    private static final double TECH_INSURANCE_FEE = 15;

    public TechDelivery(String customerName, Zone zone, double weight) {
        super(customerName, zone, weight);
    }

    // حساب السعر النهائي بإضافة رسوم التأمين على الأجهزة
    @Override
    public double calculatePrice() {
        return calculateBasicPrice() + TECH_INSURANCE_FEE;
    }
}
