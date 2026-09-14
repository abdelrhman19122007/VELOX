
package com.app.model.store;

//متجر متخصص في منتجات الملابس
public class FashionStore extends Store {

    private final String category;

    public FashionStore(String id, String name, String category) {

        // إرسال البيانات الأساسية إلى الكلاس الأب Store
        super(id, name);

        this.category = category;
    }

    // عرض بيانات المتجر بطريقة مناسبة لمتاجر الملابس
    @Override
    public void displayStoreDetails() {
        System.out.println("Fashion Store: " + getName()
                + " | Category: " + category);
    }
}

