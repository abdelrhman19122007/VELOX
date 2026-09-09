/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.model.order;

/**
 *
 * @author 3bdelr7man
 */


import com.app.enums.ProductType;
import com.app.enums.Size;
import com.app.enums.Zone;
import com.app.model.product.Product;
import com.app.service.Delivery;
import com.app.service.FoodDelivery;
import com.app.service.TechDelivery;
import com.app.service.ClothesDelivery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cart {

    private final List<Product> products;

    public Cart() {
        this.products = new ArrayList<>();
    }

    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Cannot add null product to cart");
        }
        products.add(product);
    }

    public void removeProduct(Product product) {
        products.remove(product);
    }

    public List<Product> getProducts() {
        return Collections.unmodifiableList(products);
    }

    public double calculateProductsTotal() {
        return products.stream()
                       .mapToDouble(Product::getPrice)
                       .sum();
    }

    public boolean containsType(ProductType type) {
        return products.stream()
                       .anyMatch(product -> product.getType() == type);
    }

    public double calculateTotalWeight() {
        double totalWeight = 0;
        for (Product product : products) {
            switch (product.getSize()) {
                case SMALL -> totalWeight += 1;
                case MEDIUM -> totalWeight += 2;
                case LARGE -> totalWeight += 4;
                default -> throw new IllegalArgumentException("Invalid product size");
            }
        }
        return totalWeight;
            }

    public Delivery createDelivery(String customerName, Zone zone) {
        if (products.isEmpty()) {
            throw new IllegalStateException("Cannot create delivery for an empty cart");
        }

        double totalWeight = calculateTotalWeight();

        // أولوية الشحن: الطعام ثم الإلكترونيات ثم الملابس
        if (containsType(ProductType.FOOD)) {
            return new FoodDelivery(customerName, zone, totalWeight);
        }
        if (containsType(ProductType.TECH)) {
            return new TechDelivery(customerName, zone, totalWeight);
        }
        if (containsType(ProductType.CLOTHES)) {
            return new ClothesDelivery(customerName, zone, totalWeight);
        }

        throw new IllegalStateException("No supported product type found in cart");
    }

    public void clear() {
        products.clear();
    }
}