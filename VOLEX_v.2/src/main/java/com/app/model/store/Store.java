package com.app.model.store;

public abstract class Store {
    private final String id;
    private final String name;

    public Store(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public abstract void displayStoreDetails();
}
