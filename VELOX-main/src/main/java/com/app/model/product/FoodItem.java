package com.app.model.product;
import com.app.enums.ProductType;
import com.app.enums.Size;

public class FoodItem extends Product {
    
    public FoodItem(String id, String name, double price, String large, Size size) {
        super(id, name, price,ProductType.FOOD,size);
   ;
    }


    @Override
    public void displayInfo() {
        System.out.println("Meal: " + getName() + " (" + getSize() + ") - Price: " + getPrice() + " EGP");
    }
}
