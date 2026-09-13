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
public class FoodDelivery extends Delivery {
    private static final double FOOD_HANDLING_FEE = 10;

    public FoodDelivery(String customerName, Zone zone, double weight) {
        super(customerName, zone, weight);
    }

    @Override
    public double calculatePrice() {
        return calculateBasicPrice() + FOOD_HANDLING_FEE;
    }
}