package com.app.model.order;

import java.util.ArrayList;
import com.app.discount.DiscountStrategy;
import com.app.enums.OrderStatus;
import com.app.model.product.Product;
import java.io.Serializable;
import java.time.LocalDateTime;

//----------------------------------------------
public class Order implements Serializable {
//------------------------------------------
    public static int totalOrdersCount = 0;
//---------------------------
    private static final long serialVersionUID = 1L;
    private final LocalDateTime orderDate;
    private boolean isReturned;
    //----------------------
    private final String orderId;
    private OrderStatus status;
    private final ArrayList<Product> products = new ArrayList<>();
    private DiscountStrategy discountStrategy;
    private final DeliveryAddress address;

    
       //--------------------------------------------------
        public class DeliveryAddress implements Serializable {
       //--------------------------------------------------
        private final String city;
        private final String phone;
        private static final long serialVersionUID = 1L;
        public DeliveryAddress(String city, String phone) {
            this.city = city;
            this.phone = phone;
        }

        public String getCity() { return city; }
        public String getPhone() { return phone; }
        public String getFullDetails() {
            return "City: " + city + " | Phone: " + phone;
        }
    }

    public Order(String orderId, String city, String phone) {
        this.orderId = orderId;
        this.status = OrderStatus.PENDING;
        this.address = new DeliveryAddress(city, phone);
        //----------------------------------------
        this.orderDate = LocalDateTime.now();
        this.isReturned = false;
        //----------------------------------------
        totalOrdersCount++;

    }

    public String getOrderId() { return orderId; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public DeliveryAddress getAddress() { return address; }
    public ArrayList<Product> getProducts() { return products; }
    public String getCity() { return address.getCity(); }
      public String getPhone() {return address.getPhone();}

    //--------------------------------------------------------
    public LocalDateTime getOrderDate() { return orderDate; }
    public boolean isReturned() { return isReturned; }
    public void setReturned(boolean returned) { isReturned = returned; }
    //--------------------------------------------------------
    public void addProduct(Product product) {
        if (product != null) {
            products.add(product);
            System.out.println("--> Successfully added: " + product.getName());
        }
    }

    public void addProduct(Product product, int quantity) {
        if (product != null && quantity > 0) {
            for (int i = 0; i < quantity; i++) {
                products.add(product);
            }

            System.out.println("--> Successfully added: "
                    + product.getName() + " x" + quantity);
        }
    }

    public double calculateRawTotal() {
        double total = 0;
        for (Product product : products) {
            total += product.getPrice();
        }
        return total;
    }

    public double calculateFinalTotal() {
        double total = calculateRawTotal();
        if (discountStrategy != null) {
            total = discountStrategy.applyDiscount(total);
        }
        return total;
    }

    public void setDiscountStrategy(DiscountStrategy discountStrategy) {
        this.discountStrategy = discountStrategy;
    }
    
  public void removeProduct(int index) {
    if (index >= 0 && index < products.size()) {
        products.remove(index);
    }
}
}