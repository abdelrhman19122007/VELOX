
package com.app.model.store;

//متجرمتخصص في المنتجات الإلكترونة
public class TechStore extends Store {

    private final String warranty;

    public TechStore(String id, String name, String warranty) {

        // إرسال البيانات الأساسية إلى الكلاس الأب Store
        super(id, name);

        this.warranty = warranty;
    }

    // عرض بيانات المتجر الإلكتروني ومدة الضمان
    @Override
    public void displayStoreDetails() {
        System.out.println("Tech Store: " + getName()
                + " | Warranty: " + warranty);
    }
}

