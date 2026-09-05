package com.app.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import com.app.discount.FirstOrderDiscount;
import com.app.discount.SeasonalDiscount;
import com.app.enums.OrderStatus;
import com.app.exception.InvalidOrderException;
import com.app.exception.ReturnPolicyException;
import com.app.model.order.Order;
import com.app.model.product.ClothingItem;
import com.app.model.product.ElectronicsItem;
import com.app.model.product.FoodItem;
import com.app.model.product.Product;
import com.app.model.store.FashionStore;
import com.app.model.store.Restaurant;
import com.app.model.store.TechStore;
import com.app.payment.CashOnDelivery;
import com.app.payment.CreditCardPayment;
import com.app.payment.PaymentMethod;
import com.app.payment.WalletPayment;
import com.app.service.DeliverySimulator;
import com.app.service.ReturnService;
import com.app.util.OrderRepository;
import com.app.util.PromoCodeManager;
import com.app.util.ReceiptGenerator;
import com.app.util.Response;
import com.app.service.PackagingService;
import java.util.Set;
import java.util.HashSet;
import com.app.model.order.Review;
import com.app.enums.Size;
import com.app.model.product.*;
import com.app.enums.Zone;
import com.app.model.order.Cart;
public class Main {
    public static Set<String> activePromoCodes = new HashSet<>();
    public static void printLogo() {
        System.out.println("=========================================");
        System.out.println("              V E L O X");
        System.out.println("       ALL-IN-ONE EXPRESS DELIVERY");
        System.out.println("=========================================");
    }

    public static void clearConsole() {
        for (int i = 0; i < 20; i++) {
            System.out.println();
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<Order> orderHistory = OrderRepository.loadOrders();
        ReturnService returnService = new ReturnService();
        boolean keepRunning = true;

        Restaurant orientalRest = new Restaurant("R1", "Abou Tarek & Shabrawy", "Oriental Cuisine");
        ArrayList<Product> orientalItems = new ArrayList<>();
        orientalItems.add(new FoodItem("F1", "Koshary Family Box", 120.0, "Large",Size.LARGE));
        orientalItems.add(new FoodItem("F2", "Mixed Grill Platter (1 kg)", 450.0, "Family",Size.LARGE));
        orientalItems.add(new FoodItem("F3", "Beef Shawarma Wrap", 95.0, "Medium",Size.MEDIUM));
        orientalItems.add(new FoodItem("F4", "Chicken Crepe Crunchy", 110.0, "Large",Size.LARGE));
        orientalItems.add(new FoodItem("F5", "Molokhia with Half Chicken", 160.0, "Standard",Size.MEDIUM));

        Restaurant westernRest = new Restaurant("R2", "Buffalo & Pizza Hut", "Western Cuisine");
        ArrayList<Product> westernItems = new ArrayList<>();
        westernItems.add(new FoodItem("F6", "Pizza Super Supreme", 260.0, "Large",Size.LARGE));
        westernItems.add(new FoodItem("F7", "Double Mushroom Beef Burger", 180.0, "Medium", Size.MEDIUM));
        westernItems.add(new FoodItem("F8", "Crispy Chicken Strips Meal", 165.0, "Large",Size.LARGE));
        westernItems.add(new FoodItem("F9", "Italian Pasta Alfredo", 140.0, "Standard",Size.LARGE));
        westernItems.add(new FoodItem("F10", "Cheesy Garlic Bread", 75.0, "Small", Size.SMALL));

        FashionStore luxuryStore = new FashionStore("S1", "Lacoste Luxury", "Luxury Perfumes & Accessories");
        ArrayList<Product> luxuryItems = new ArrayList<>();
        luxuryItems.add(new ClothingItem("C1", "Lacoste French Perfume (100ml)", 4500.0, "Fragrance",Size.MEDIUM));
        luxuryItems.add(new ClothingItem("C2", "Classic Croco Polo Shirt", 3800.0, "White", Size.MEDIUM));
        luxuryItems.add(new ClothingItem("C3", "Genuine Leather Belt Set", 2200.0, "Black", Size.MEDIUM));

        FashionStore midSportStore = new FashionStore("S2", "Adidas Sport", "Sportswear & Footwear");
        ArrayList<Product> sportItems = new ArrayList<>();
        sportItems.add(new ClothingItem("C4", "Ultraboost Running Sneakers", 2400.0, "Black/Red", Size.MEDIUM));
        sportItems.add(new ClothingItem("C5", "Athletic Tracksuit Set", 1750.0, "Navy Blue", Size.MEDIUM));
        sportItems.add(new ClothingItem("C6", "Sport Cap & Wristbands", 450.0, "White", Size.SMALL));

        FashionStore budgetStore = new FashionStore("S3", "Zara Casual", "Everyday Wear & Caps");
        ArrayList<Product> budgetItems = new ArrayList<>();
        budgetItems.add(new ClothingItem("C7", "Casual Denim Jacket", 1200.0, "Blue Denim", Size.MEDIUM));
        budgetItems.add(new ClothingItem("C8", "Basic Cotton T-Shirt Pack", 450.0, "Grey", Size.MEDIUM));
        budgetItems.add(new ClothingItem("C9", "Summer Bucket Hat", 300.0, "Beige", Size.SMALL));

        TechStore luxuryTech = new TechStore("T1", "Apple Flagship Store", "2 Years Warranty");
        ArrayList<Product> appleItems = new ArrayList<>();
        appleItems.add(new ElectronicsItem("E1", "iPhone 15 Pro Max 256GB", 65000.0, "Apple", Size.MEDIUM));
        appleItems.add(new ElectronicsItem("E2", "MacBook Air M2 13-inch", 52000.0, "Apple",Size.LARGE));
        appleItems.add(new ElectronicsItem("E3", "AirPods Pro 2nd Gen", 11500.0, "Apple", Size.SMALL));

        TechStore midTech = new TechStore("T2", "Samsung Smart Hub", "1 Year Warranty");
        ArrayList<Product> samsungItems = new ArrayList<>();
        samsungItems.add(new ElectronicsItem("E4", "Samsung Galaxy S24 Ultra", 48000.0, "Samsung", Size.MEDIUM));
        samsungItems.add(new ElectronicsItem("E5", "Smart Watch Galaxy Watch 6", 8500.0, "Samsung", Size.SMALL));
        samsungItems.add(new ElectronicsItem("E6", "Wireless Fast Charging Pad", 950.0, "Samsung", Size.SMALL));

        while (keepRunning) {
            clearConsole();
            printLogo();

            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Create New Order");
            System.out.println("2. Return Order");
            System.out.println("3. Exit");
            int mainMenuChoice = readInt(scanner, "Choose Option (1-3): ");

            switch (mainMenuChoice) {
                case 1 -> {
                    System.out.print("\nEnter Delivery City: ");
                    String inputCity = scanner.nextLine();
                    Zone selectedZone = parseZone(inputCity);
                    System.out.print("Enter Phone Number: ");
                    String phone = scanner.nextLine().trim();
                    //  إدخال الميزانية المتاحة مع العميل
                    System.out.print("Enter your available Budget (EGP): ");
                    double userBudget = scanner.nextDouble();
                    scanner.nextLine(); 
                    
                   String generatedOrderId = "ORD-" + getNextOrderId(orderHistory);
                   Order order = new Order(generatedOrderId, selectedZone.name(), phone);
                    boolean shopping = true;

                    while (shopping) {
                        System.out.println("\nCart Items: " + order.getProducts().size());
                        System.out.println("Cart Total: " + order.calculateRawTotal() + " EGP");
                        System.out.println("1. Food & Restaurants");
                        System.out.println("2. Fashion & Clothes");
                        System.out.println("3. Tech & Electronics");
                        System.out.println("4. Proceed to Checkout");
                        int mainChoice = readInt(scanner, "Choose Option: ");

                        switch (mainChoice) {
                            case 1 -> {boolean foodMenu = true;
                              while (foodMenu) {
                                  
                                System.out.println("\n--- FOOD & RESTAURANTS ---");
                                System.out.println("\n1. " + orientalRest.getName());
                                System.out.println("2. " + westernRest.getName());
                                System.out.println("0. Back to Shopping Menu");
                                int restPick = readInt(scanner, "Select Restaurant: ");
                               if (restPick == 0) {
                                    foodMenu = false;
                                } else if (restPick == 1 || restPick == 2) {
                                    ArrayList<Product> list = (restPick == 1) ? orientalItems : westernItems;
                                    addSelectedProduct(scanner, order, list);
                                } else {
                                    System.out.println("Invalid restaurant selection.");
                                }
                            }}
                                case 2 -> {
                                boolean fashionMenu = true;
                                while (fashionMenu) {
                                    System.out.println("\n--- FASHION & CLOTHES ---");
                                    System.out.println("1. " + luxuryStore.getName());
                                    System.out.println("2. " + midSportStore.getName());
                                    System.out.println("3. " + budgetStore.getName());
                                    System.out.println("0. Back to Shopping Menu");
                                    int brandPick = readInt(scanner, "Select Brand: ");

                                    if (brandPick == 0) {
                                        fashionMenu = false;
                                    } else if (brandPick >= 1 && brandPick <= 3) {
                                        ArrayList<Product> list = (brandPick == 1) ? luxuryItems : (brandPick == 2) ? sportItems : budgetItems;
                                        addSelectedProduct(scanner, order, list);
                                    } else {
                                        System.out.println("Invalid fashion store selection.");
                                    }
                                }
                            }
                            case 3 -> {
                                    boolean techMenu = true;
                                    while (techMenu) {
                                        System.out.println("\n--- TECH & ELECTRONICS ---");
                                        System.out.println("1. " + luxuryTech.getName());
                                        System.out.println("2. " + midTech.getName());
                                        System.out.println("0. Back to Shopping Menu");
                                        int techPick = readInt(scanner, "Select Store: ");

                                        if (techPick == 0) {
                                            techMenu = false;
                                        } else if (techPick == 1 || techPick == 2) {
                                            ArrayList<Product> list = (techPick == 1) ? appleItems : samsungItems;
                                            addSelectedProduct(scanner, order, list);
                                        } else {
                                            System.out.println("Invalid tech store selection.");
                                        }
                                    }
                                }
                            case 4 -> {
                                if (order.getProducts().isEmpty()) {
                                    System.out.println("Cart is empty! Add items first.");
                                } else {
                                    shopping = false;
                                }
                            }
                            default -> System.out.println("Invalid option!");
                        }
                    }

                    try {
                        validateOrder(order);

                        Cart tempCart = new Cart();
                        for (Product p : order.getProducts()) {
                            tempCart.addProduct(p); // أو اسم دالة إضافة المنتج للسلة لديك
                        }
                      Zone zone = parseZone(order.getCity());
                       double deliveryFee = tempCart.createDelivery(order.getPhone(), zone).calculatePrice();
                        System.out.print("\nEnter Promo Code (or press Enter to skip): ");
                        String promoInput = scanner.nextLine().trim();

                        if (!promoInput.isEmpty()) {
                            String code = promoInput.toUpperCase();

                            // 1. فحص كود الخصم التعويضي الخاص بالشكاوى
                            if (activePromoCodes.contains(code)) {
                                order.setDiscountStrategy(new SeasonalDiscount()); // خصم الشكاوى
                                activePromoCodes.remove(code); // إلغاء الكود بعد الاستخدام
                                System.out.println("--> Compensation promo code applied successfully!");
                            } 
                            // 2. فحص الأكواد الأساسية للسيستم
                            else if (PromoCodeManager.isValidCode(promoInput)) {
                                if (code.equals("FIRSTORDER")) {
                                    order.setDiscountStrategy(new FirstOrderDiscount());
                                    System.out.println("FIRSTORDER applied: 35% discount.");
                                } else if (code.equals("VELOX10")) {
                                    order.setDiscountStrategy(new SeasonalDiscount());
                                    System.out.println("VELOX10 applied: 15% discount.");
                                } else {
                                    deliveryFee = 0;
                                    System.out.println("FREESHIP applied: delivery is free.");
                                }
                            } 
                            // 3. في حالة إدخال كود غير صحيح
                            else {
                                System.out.println("Invalid or expired promo code.");
                            }
                        }

                        // 1. خطوة اختيار نوع التغليف
                          double unitPackagingFee = 0.0;
                         String packagingType = "None";

                         System.out.println("\nSelect Packaging Type:");
                         System.out.println("1. Standard Packaging (10 EGP / item)");
                         System.out.println("2. Gift Packaging (25 EGP / item)");
                         System.out.println("3. No Packaging");
                         System.out.print("Choice: ");

                         int packChoice = scanner.nextInt();
                         scanner.nextLine();

                         if (packChoice == 1) {
                             unitPackagingFee = 10.0; // سعر تغليف القطعة الواحدة
                             packagingType = "Standard Packaging";
                         } else if (packChoice == 2) {
                             unitPackagingFee = 25.0; // سعر تغليف القطعة الواحدة للهدايا
                             packagingType = "Gift Packaging";
                         }
                         // 2. حساب إجمالي عدد القطع مضروباً في سعر التغليف
                        int totalItemsCount = order.getProducts().size(); 
                        double totalPackagingFee = unitPackagingFee * totalItemsCount;
                         double amountToPay = order.calculateFinalTotal() + deliveryFee + totalPackagingFee;
                        boolean isPaymentSuccessful = false;

                        
                        System.out.println("1. Wallet");
                        System.out.println("2. Credit Card");
                        System.out.println("3. Cash on Delivery");
                        int payChoice = readInt(scanner, "Select Payment Method: ");

                      
                       // 1. إنشاء كائن وسيلة الدفع بناءً على اختيار المستخدم
                        PaymentMethod payment;

                        if (payChoice == 1) {
                            // طلب رقم المحفظة من المستخدم
                            System.out.print("Enter your Wallet Phone Number: ");
                            String walletNumber = scanner.nextLine();

                            // إنشاء كائن المحفظة برقم المحفظة والرصيد المتاح
                            payment = new WalletPayment(walletNumber, userBudget);

                        } else if (payChoice == 2) {
                            // طلب رقم كارت الائتمان من المستخدم
                            System.out.print("Enter your 16-digit Credit Card Number: ");
                            String cardNumber = scanner.nextLine();

                            // إنشاء كائن الكريدت كارد برقم الكارت الذي أدخله المستخدم
                           payment = new CreditCardPayment(cardNumber, userBudget);;

                        } else {
                            // الدفع عند الاستلام لا يتطلب إدخال أرقام
                           payment = new CashOnDelivery(userBudget);
                        }
                        payment.pay(order.calculateFinalTotal());
                        // 2. تنفيذ الدفع والتحقق من الحالة
                       if (!payment.getPaymentStatus()) {
                        System.out.println("\n>>> Would you like to remove items from your cart to reduce the total? (1: Yes / 2: No)");
                        int choice = scanner.nextInt();
                        scanner.nextLine();

                        if (choice == 1) {
                            manageCart(scanner, order); // تحويله لواجهة حذف المنتجات من السلة
                        } else {
                            System.out.println("Order cancelled.");
                            break;
                        }
}
                      
                       

                        // 2. خطوة إنشاء وطباعة الفاتورة
                        String receipt = ReceiptGenerator.generateReceipt(order, deliveryFee, totalPackagingFee, packagingType);
                        System.out.println(receipt);
                        OrderRepository.saveReceiptText(receipt);
                         
                       
                        
                         //-----------------------------------
                       DeliverySimulator simulator = new DeliverySimulator();
                        simulator.startLiveTracking(order);
                          // ==========================================
                        // خدمة العملاء والشكاوى بعد الاستلام
                        // ==========================================
                        System.out.println("\nDo you have any issues or complaints about your order?");
                         System.out.println("1. No, everything is fine");
                        System.out.println("2. Yes, I want to submit a complaint");                       
                        int complaintCheck = readInt(scanner, "Select choice (1-2): ");

                        if (complaintCheck == 2) {
                            handleComplaintSection(scanner);
                        } else {
                            System.out.println("\nThank you for shopping with us! Have a great day.");
                        }
                        //===========================================
                        // 1. قراءة التقييم من 1 إلى 5
                        int userRating = readInt(scanner, "Please rate your experience from 1 to 5: ");
                        while (userRating < 1 || userRating > 5) {
                            System.out.println("Invalid rating. Please enter a number between 1 and 5.");
                            userRating = readInt(scanner, "Please rate your experience from 1 to 5: ");
                        }

                       

                        // 3. قراءة التعليق النصي
                        System.out.print("Write any comments (optional): ");
                        String userComment = scanner.nextLine();

                        // 4. إنشاء كائن من كلاس Review الأصلي واستدعاء دالة العرض
                        Review myReview = new Review(userRating, userComment);
                        myReview.displayReview();
                       
                        // ==========================================
                
                        
                        // مسح الشاشة بعد انتهاء التوصيل
                        clearConsole();
                        printLogo();

                        Response<Order> response = new Response<>(order, "ORDER_COMPLETED_SUCCESSFULLY");
                        System.out.println("\n[VELOX SERVER]: " + response.getMessage());
                        System.out.println("Final Order Status: " + response.getData().getStatus() + "\n");

                        // عرض الفاتورة الأخيرة
                        System.out.println(receipt);

                        // حفظ الفاتورة في الملفات
                        orderHistory.add(order);
                        OrderRepository.saveOrders(orderHistory);
                        System.out.println("[DATABASE]: Order saved successfully!");

                        // الانتظار لقراءة الفاتورة قبل العودة للمنيو
                        System.out.print("\nPress Enter to return to Main Menu...");
                        scanner.nextLine();

                    }
                    catch (InvalidOrderException e) {
                        System.out.println("\n[ORDER ERROR]: " + e.getMessage());
                    }}

                case 2 -> {
                    System.out.println("\n=== RETURN REQUEST ===");
                    System.out.print("Enter Order ID to return: ");
                    String searchId = scanner.nextLine().trim();

                    Order foundOrder = null;
                    for (Order o : orderHistory) {
                        if (o.getOrderId().equalsIgnoreCase(searchId)) {
                            foundOrder = o;
                            break;
                        }
                    }

                    if (foundOrder == null) {
                        System.out.println("[NOT FOUND] Order ID does not exist!");
                    } else {
                        try {
                            Response<Double> returnResponse = returnService.processReturn(foundOrder);
                            OrderRepository.saveOrders(orderHistory);
                            System.out.println("\n[RETURN SUCCESS]: " + returnResponse.getMessage());
                        } catch (ReturnPolicyException e) {
                            System.out.println("\n[RETURN REJECTED]: " + e.getMessage());
                        }
                    }
                }

                case 3 -> {
                    keepRunning = false;
                    System.out.println("\nThank you for using VELOX!");
                }

                default -> System.out.println("Invalid choice!");
            }
        }

        scanner.close();
    }

    private static void addSelectedProduct(Scanner scanner, Order order, ArrayList<Product> products) {
        System.out.println("\n=============================================================================");
        System.out.printf(Locale.US, "%-4s | %-6s | %-35s | %-12s%n", "#", "ID", "Product Name", "Price");
        System.out.println("-----------------------------------------------------------------------------");

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            System.out.printf(Locale.US, "%-4d | %-6s | %-35s | %-10.2f EGP%n", 
                              (i + 1), p.getId(), p.getName(), p.getPrice());
        }
        System.out.println("=============================================================================");

        int choice = readInt(scanner, "\nSelect item to add: ");

        if (choice >= 1 && choice <= products.size()) {
            int quantity = readInt(scanner, "Enter quantity: ");

            if (quantity > 0) {
                order.addProduct(products.get(choice - 1), quantity);
                System.out.println("\n[SUCCESS] Item added to cart successfully!");
            } else {
                System.out.println("\n[ERROR] Quantity must be greater than 0.");
            }
        } else {
            System.out.println("\n[ERROR] Invalid item selection.");
        }
    }

    private static int readInt(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
       

   
    }

    public static void handleComplaintSection(Scanner scanner) {
        System.out.println("\n--- COMPLAINT & SUPPORT SYSTEM ---");
        System.out.print("Enter Order ID related to your complaint: ");
        String orderId = scanner.next();
        scanner.nextLine();

        System.out.print("Please describe your problem: ");
        String details = scanner.nextLine();

        String complaintId = "CMP-" + (int)(Math.random() * 9000 + 1000);
        com.app.model.order.Complaint complaint = new com.app.model.order.Complaint(complaintId, orderId, details);

        com.app.util.ComplaintRepository.saveComplaintToFile(complaint);

        System.out.println("\n==========================================");
        System.out.println("Complaint Submitted Successfully!");
        System.out.println("Your Complaint ID: " + complaint.getComplaintId());
        System.out.println("==========================================");

        System.out.println("\n--- LIVE COMPLAINT TRACKING ---");
        try {
            System.out.println("Status: " + complaint.getStatus().getDescription());
            Thread.sleep(1500);

            complaint.setStatus(com.app.enums.ComplaintStatus.IN_REVIEW);
            System.out.println("--> Update: " + complaint.getStatus().getDescription());
            Thread.sleep(2000);

            complaint.setStatus(com.app.enums.ComplaintStatus.RESOLVED);
            System.out.println("--> Final Status: " + complaint.getStatus().getDescription());
            System.out.println("--> Resolution: Our support team has reviewed your issue and processed a solution.");
            String generatedPromo = "SORRY" + (int)(Math.random() * 9000 + 1000);
            activePromoCodes.add(generatedPromo);

            System.out.println("==========================================");
            System.out.println("? Compensation Discount Generated!");
            System.out.println("Use Code: " + generatedPromo + " on your next order for 10% OFF.");
            System.out.println("==========================================");
        } catch (InterruptedException e) {
            System.out.println("Tracking interrupted.");
        }
    }

    private static void validateOrder(Order order) throws InvalidOrderException {
        if (order.getProducts().isEmpty()) {
            throw new InvalidOrderException("Cannot process checkout: Order cart is empty.");
        }
        if (order.getCity().isEmpty()) {
            throw new InvalidOrderException("Cannot process checkout: Delivery city is missing.");
        }
        if (order.getAddress().getPhone().isEmpty()) {
            throw new InvalidOrderException("Cannot process checkout: Phone number is missing.");
        }
    }
   
    public static void manageCart(Scanner scanner, Order order) {
    while (true) {
        if (order.getProducts().isEmpty()) {
            System.out.println("\n Your cart is empty!");
            break;
        }

        System.out.println("\n---  YOUR CART ---");
      for (int i = 0; i < order.getProducts().size(); i++) {
    Product p = order.getProducts().get(i);
    System.out.printf(Locale.US, "%d. %s - %.2f EGP%n", (i + 1), p.getName(), p.getPrice());
}
        System.out.println("--------------------");
        System.out.println("1. Remove an item");
        System.out.println("2. Proceed to Checkout");

        int choice = readInt(scanner, "Choice: ");

        if (choice == 1) {
            int itemNum = readInt(scanner, "Enter item number to remove: ");

            if (itemNum > 0 && itemNum <= order.getProducts().size()) {
                Product removed = order.getProducts().get(itemNum - 1);
                order.removeProduct(itemNum - 1);
                System.out.println("--> Removed: " + removed.getName());
            } else {
                System.out.println("--> Invalid item number.");
            }
        } else if (choice == 2) {
            break;
        }
    }
    } 
    
    public static Zone parseZone(String input) {
    if (input == null || input.trim().isEmpty()) {
        return Zone.CAIRO;
    }

    String clean = input.trim().toUpperCase();

    for (Zone z : Zone.values()) {
        if (z.name().equalsIgnoreCase(clean)) {
            return z;
        }
    }

    if (clean.startsWith("ALEX") || clean.startsWith("AL") || clean.startsWith("A")) {
        return Zone.ALEXANDRIA;
    }
    if (clean.startsWith("GIZ") || clean.startsWith("G")) {
        return Zone.GIZA;
    }
    if (clean.startsWith("CAI") || clean.startsWith("KAI") || clean.startsWith("C")) {
        return Zone.CAIRO;
    }
    if (clean.startsWith("DAM") || clean.startsWith("DOM") || clean.startsWith("D")) {
        return Zone.DAMIETTA;
    }

    for (Zone z : Zone.values()) {
        if (z.name().contains(clean) || clean.contains(z.name())) {
            return z;
        }
    }

    return Zone.CAIRO; // في حالة إدخال رمز خاطئ تماماً كـ L يتحول تلقائياً للقاهرة
}
    
    public static int getNextOrderId(List<Order> orderHistory) {
    int maxId = 100; // البداية لتكون أول فاتورة 101
    
    if (orderHistory != null) {
        for (Order order : orderHistory) {
            try {
                // استخراج الرقم من النص "ORD-105" ليصبح 105
                String numStr = order.getOrderId().replace("ORD-", "").trim();
                int num = Integer.parseInt(numStr);
                if (num > maxId) {
                    maxId = num;
                }
            } catch (Exception e) {
                // لتجاهل أي ID مكتوب بشكل غير قياسي
            }
        }
    }
    return maxId + 1;
}
}    
    
