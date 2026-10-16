
package com.app.model.product;

import com.app.enums.ProductType;
import com.app.enums.Size;

// يمثل منتجًا إلكترونيًا ويرث الخصائص الأساسية من Product
public class ElectronicsItem extends Product {

    private final String brand;

    public ElectronicsItem(String id, String name, double price, String brand, Size size) {

        // إرسال البيانات الأساسية إلى Constructor الكلاس الأب
        super(id, name, price, ProductType.TECH, size);

        this.brand = brand;
    }

    // عرض بيانات المنتج الإلكتروني
    @Override
    public void displayInfo() {
        System.out.println("Electronics: " + getName()
                + " [" + brand + "] - Price: " + getPrice() + " EGP");
    }
}