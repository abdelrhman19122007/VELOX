
package com.app.model.product;

import com.app.enums.ProductType;
import com.app.enums.Size;

// يمثل منتجًا من نوع الملابس ويرث الخصائص الأساسية من Product
public class ClothingItem extends Product {

    private final String color;

    public ClothingItem(String id, String name, double price, String color, Size size) {

        // استدعاء Constructor الأب لتخزين البيانات المشتركة للمنتجات
        super(id, name, price, ProductType.CLOTHES, size);

        this.color = color;
    }

    // عرض بيانات المنتج بطريقة مناسبة لمنتجات الملابس
    @Override
    public void displayInfo() {
        System.out.println("Fashion Item: " + getName()
                + " (" + color + ") - Price: " + getPrice() + " EGP");
    }
}
