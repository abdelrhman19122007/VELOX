/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.service;

/**
 *
 * @author 3bdelr7man
 */
import com.app.enums.Governorate;
import com.app.enums.Zone;

// الكلاس الأساسي لإدارة عمليات التوصيل ورسومها
public abstract class Delivery {
    private String customerName;
    private Zone zone;
    private Governorate governorate;
    private double weight;

    // المشيد مع التحقق من صحة البيانات المدخلة
    public Delivery(String customerName, Zone zone, double weight) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        if (zone == null)
            throw new IllegalArgumentException("Delivery zone cannot be null");
        if (weight <= 0)
            throw new IllegalArgumentException("Weight must be greater than zero");

        this.customerName = customerName;
        this.zone = zone;
        this.governorate = null;
        this.weight = weight;
    }

    // مشيد بالمحافظة: السعر يؤخذ من سعر شحن المحافظة مباشرة
    public Delivery(String customerName, Governorate governorate, double weight) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        if (governorate == null)
            throw new IllegalArgumentException("Governorate cannot be null");
        if (weight <= 0)
            throw new IllegalArgumentException("Weight must be greater than zero");

        this.customerName = customerName;
        this.governorate = governorate;
        this.zone = toZone(governorate);
        this.weight = weight;
    }

    private static Zone toZone(Governorate governorate) {
        try {
            return Zone.valueOf(governorate.name());
        } catch (IllegalArgumentException e) {
            return null; // المحافظات خارج النطاق الأصلي ليس لها Zone
        }
    }

    public String getCustomerName() {
        return customerName;
    }

    public Zone getZone() {
        return zone;
    }

    public Governorate getGovernorate() {
        return governorate;
    }

    public double getWeight() {
        return weight;
    }

    // حساب السعر الأساسي بناءً على المنطقة والوزن
    protected double calculateBasicPrice() {
        double areaPrice;
        if (governorate != null) {
            // كل محافظة مسعرة بسعر شحنها الخاص
            areaPrice = governorate.getShippingPrice();
        } else {
            areaPrice = switch (zone) {
                case CAIRO -> 20;
                case GIZA -> 25;
                case ALEXANDRIA -> 40;
                case DAMIETTA -> 60;
                default -> throw new IllegalStateException("Unexpected value: " + (zone));
            };
        }

        double weightPrice;
        if (weight <= 2)
            weightPrice = 0;
        else if (weight <= 5)
            weightPrice = 10;
        else
            weightPrice = 50;

        return areaPrice + weightPrice;
    }

    // دالة مجردة لحساب السعر النهائي حسب نوع التوصيل
    public abstract double calculatePrice();
}
