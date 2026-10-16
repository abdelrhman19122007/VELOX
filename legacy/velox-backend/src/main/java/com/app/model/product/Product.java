
package com.app.model.product;

import com.app.enums.ProductType;
import com.app.enums.Size;
import java.io.Serializable;

// كلاس أساسي مجرد لجميع أنواع المنتجات في النظام
public abstract class Product implements Serializable {

    // مطلوب لاستخدام Serialization
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final double price;
    private ProductType type;
    private Size size;

    public Product(String id, String name, double price, ProductType type, Size size) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.type = type;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public ProductType getType() {
        return type;
    }

    public Size getSize() {
        return size;
    }

    // كل نوع من المنتجات يحدد بنفسه طريقة عرض بياناته
    public abstract void displayInfo();
}