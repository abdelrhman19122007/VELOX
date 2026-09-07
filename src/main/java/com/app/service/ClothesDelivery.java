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

// كلاس حساب تكلفة توصيل ملابس
public class ClothesDelivery extends Delivery {
    // المشيد لإسناد البيانات الأساسية للتوصيل
    public ClothesDelivery(String customerName, Zone zone, double weight) {
        super(customerName, zone, weight);
    }

    // حساب السعر النهائي لتوصيل الملابس بدون رسوم إضافية
    @Override
    public double calculatePrice() {
        return calculateBasicPrice();
    }
}
