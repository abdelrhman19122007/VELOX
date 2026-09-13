package com.app.model.store;

public class Restaurant extends Store {
    private final String cuisineType;

    public Restaurant(String id, String name, String cuisineType) {
        super(id, name);
        this.cuisineType = cuisineType;
    }

    @Override
    public void displayStoreDetails() {
        System.out.println("Restaurant: " + getName() + " | Cuisine: " + cuisineType);
    }
}
