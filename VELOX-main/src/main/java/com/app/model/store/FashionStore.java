package com.app.model.store;

public class FashionStore extends Store {
    private final String category;

    public FashionStore(String id, String name, String category) {
        super(id, name);
        this.category = category;
    }

    @Override
    public void displayStoreDetails() {
        System.out.println("Fashion Store: " + getName() + " | Category: " + category);
    }
}
