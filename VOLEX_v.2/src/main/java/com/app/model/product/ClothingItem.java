package com.app.model.product;
import com.app.enums.ProductType;
import com.app.enums.Size;
public class ClothingItem extends Product {
    private final String color;

    public ClothingItem(String id, String name, double price, String color,Size size) {
        super(id, name, price,ProductType.CLOTHES,size);
        this.color = color;
    }

    @Override
    public void displayInfo() {
        System.out.println("Fashion Item: " + getName() + " (" + color + ") - Price: " + getPrice() + " EGP");
    }
}
