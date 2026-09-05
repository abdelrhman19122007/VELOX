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
public abstract class Delivery {
      private String customerName;
    private Zone zone;
    private double weight;

    public Delivery(String customerName, Zone zone, double weight) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty");
        }
        if (zone == null) throw new IllegalArgumentException("Delivery zone cannot be null");
        if (weight <= 0) throw new IllegalArgumentException("Weight must be greater than zero");

        this.customerName = customerName;
        this.zone = zone;
        this.weight = weight;
    }

    public String getCustomerName() { return customerName; }
    public Zone getZone() { return zone; }
    public double getWeight() { return weight; }

    protected double calculateBasicPrice() {
        double zonePrice = switch (zone) {
            case CAIRO -> 20;
            case GIZA -> 25;
            case ALEXANDRIA -> 40;
            case DAMIETTA ->60;
            default -> throw new IllegalStateException("Unexpected value: " + (zone));
        };

        double weightPrice;
        if (weight <= 2) weightPrice = 0;
        else if (weight <= 5) weightPrice = 10;
        else weightPrice = 50;

        return zonePrice + weightPrice;
    }

    public abstract double calculatePrice();
}

