package com.app.model.product;

import com.app.enums.ProductType;
import com.app.enums.Size;

// يمثل منتجًا غذائيًا ويرث الخصائص الأساسية من Product
public class FoodItem extends Product {

    public FoodItem(String id, String name, double price, String large, Size size) {

        // إرسال البيانات الأساسية إلى Constructor الكلاس الأب
        super(id, name, price, ProductType.FOOD, size);
    }

    // عرض بيانات الوجبة للمستخدم
    @Override
    public void displayInfo() {
        System.out.println("Meal: " + getName()
                + " (" + getSize() + ") - Price: "
                + getPrice() + " EGP");
    }
}
