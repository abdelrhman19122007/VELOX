
package com.app.model.store;

// الكلاس الأساسي المشترك بين جميع أنواع المتاجر
public abstract class Store {

    private final String id;
    private final String name;

    public Store(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // كل نوع متجر يحدد بنفسه طريقة عرض بياناته
    public abstract void displayStoreDetails();
}
