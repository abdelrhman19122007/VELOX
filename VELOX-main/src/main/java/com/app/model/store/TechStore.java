package com.app.model.store;

public class TechStore extends Store {
    private final String warranty;

    public TechStore(String id, String name, String warranty) {
        super(id, name);
        this.warranty = warranty;
    }

    @Override
    public void displayStoreDetails() {
        System.out.println("Tech Store: " + getName() + " | Warranty: " + warranty);
    }
}
