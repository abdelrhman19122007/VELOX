package com.app.model.product;
import com.app.enums.ProductType;
import com.app.enums.Size;
public class ElectronicsItem extends Product {
    private final String brand;

    public ElectronicsItem(String id, String name, double price, String brand, Size size) {
        super(id, name, price,ProductType.TECH,size);
        this.brand = brand;
    }

    @Override
    public void displayInfo() {
        System.out.println("Electronics: " + getName() + " [" + brand + "] - Price: " + getPrice() + " EGP");
    }
}
