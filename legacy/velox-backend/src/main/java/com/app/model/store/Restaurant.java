
package com.app.model.store;

// يمثل مطعمًا ويرث الخصائص الأساسية من Store
public class Restaurant extends Store {

    private final String cuisineType;

    public Restaurant(String id, String name, String cuisineType) {

        // إرسال البيانات الأساسية إلى الكلاس الأب Store
        super(id, name);

        this.cuisineType = cuisineType;
    }

    // عرض بيانات المطعم ونوع المطبخ الخاص به
    @Override
    public void displayStoreDetails() {
        System.out.println("Restaurant: " + getName()
                + " | Cuisine: " + cuisineType);
    }
}
