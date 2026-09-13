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
import com.app.util.StoreRepository;
// نقطة بداية التشغيل الأساسية للبرنامج وتحميل البيانات المخزنة
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
    // تهيئة القوائم والمنتجات للمطاعم ومحلات الملابس والإلكترونيات
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<Order> orderHistory = OrderRepository.loadOrders();
        ReturnService returnService = new ReturnService();
        boolean keepRunning = true;
        StoreRepository storeRepo = new StoreRepository();
        
                   
        // المطاعم
        Restaurant orientalRest = new Restaurant("R1", "Oriental", "Egyptian Cuisine");
        Restaurant westernRest  = new Restaurant("R2", "Western", "International Cuisine");

        // متاجر الملابس
        FashionStore luxuryStore   = new FashionStore("S1", "Luxury Store", "Luxury");
        FashionStore midSportStore = new FashionStore("S2", "Sport Store", "Sportswear");
        FashionStore budgetStore   = new FashionStore("S3", "Budget Store", "Budget");

        // متاجر الإلكترونيات
        TechStore luxuryTech = new TechStore("T1", "Apple Store", "1 Year Warranty");
        TechStore midTech    = new TechStore("T2", "Samsung Store", "2 Years Warranty");
        // الحلقة التكرارية الرئيسية لتنقل المستخدم بين خدمات التطبيق
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
                    // إدخال بيانات العميل (المدينة، الهاتف، الميزانية)
                    System.out.print("\nEnter Delivery City: ");
                    String inputCity = scanner.nextLine();
                    Zone selectedZone = parseZone(inputCity);
                    System.out.print("Enter Phone Number: ");
                    String phone = scanner.nextLine().trim();
                    // إدخال الميزانية المتاحة مع العميل
                    System.out.print("Enter your available Budget (EGP): ");
                    double userBudget = scanner.nextDouble();
                    scanner.nextLine();

                    String generatedOrderId = "ORD-" + getNextOrderId(orderHistory);
                    // إنشاء كائن طلب جديد
                    Order order = new Order(generatedOrderId, selectedZone.name(), phone);
                    boolean shopping = true;
                    // عرض أقسام المتاجر المتاحة واختيار المنتجات وإضافتها للسلة
                    while (shopping) {
                        System.out.println("\nCart Items: " + order.getProducts().size());
                        System.out.println("Cart Total: " + order.calculateRawTotal() + " EGP");
                        System.out.println("1. Food & Restaurants");
                        System.out.println("2. Fashion & Clothes");
                        System.out.println("3. Tech & Electronics");
                        System.out.println("4. Proceed to Checkout");
                        int mainChoice = readInt(scanner, "Choose Option: ");

                        switch (mainChoice) {
                            case 1 -> {
                                boolean foodMenu = true;
                                while (foodMenu) {

                                    System.out.println("\n--- FOOD & RESTAURANTS ---");
                                    System.out.println("\n1. " + orientalRest.getName());
                                    System.out.println("2. " + westernRest.getName());
                                    System.out.println("0. Back to Shopping Menu");
                                    int restPick = readInt(scanner, "Select Restaurant: ");
                                    if (restPick == 0) {
                                        foodMenu = false;
                                    } else if (restPick == 1) { 
                                        // 1 is the ID for Abou Tarek (Oriental)
                                         ArrayList<Product> items = storeRepo.getProductsByStoreId(1);
                                         addSelectedProduct(scanner, order, items);
                                     } else if (restPick == 2) { 
                                         // 4 is the ID for Buffalo (Western)
                                         ArrayList<Product> items = storeRepo.getProductsByStoreId(4);
                                         addSelectedProduct(scanner, order, items);
                                     } else {
                                        System.out.println("Invalid restaurant selection.");
                                    }
                                }
                            }
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
                                   } else if (brandPick == 1) { 
                                        // 5 is the ID for Lacoste
                                        ArrayList<Product> items = storeRepo.getProductsByStoreId(5);
                                        addSelectedProduct(scanner, order, items);
                                    } else if (brandPick == 2) { 
                                        // 6 is the ID for Adidas
                                        ArrayList<Product> items = storeRepo.getProductsByStoreId(6);
                                        addSelectedProduct(scanner, order, items);
                                    } else if (brandPick == 3) { 
                                        // 2 is the ID for Zara
                                        ArrayList<Product> items = storeRepo.getProductsByStoreId(2);
                                        addSelectedProduct(scanner, order, items);
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
                                    } else if (techPick == 1) { 
                                        // 3 is the ID for Apple
                                        ArrayList<Product> items = storeRepo.getProductsByStoreId(3);
                                        addSelectedProduct(scanner, order, items);
                                    } else if (techPick == 2) { 
                                        // 7 is the ID for Samsung
                                        ArrayList<Product> items = storeRepo.getProductsByStoreId(7);
                                        addSelectedProduct(scanner, order, items);
                                    }else {
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
                        // التأكد من صحة بيانات الطلب
                        validateOrder(order);

                        Cart tempCart = new Cart();
                        for (Product p : order.getProducts()) {
                            tempCart.addProduct(p); // أو اسم دالة إضافة المنتج للسلة لديك
                        }
                        Zone zone = parseZone(order.getCity());
                        double deliveryFee = tempCart.createDelivery(order.getPhone(), zone).calculatePrice();
                        System.out.print("\nEnter Promo Code (or press Enter to skip): ");
                        String promoInput = scanner.nextLine().trim();
                        // تطبيق أكواد الخصم والتوصيل المجاني
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

                       // تحديد نوع التغليف وحساب التكلفة
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
                        // تحديد طريقة الدفع (محفظة، كارت، كاش)
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
                            payment = new CreditCardPayment(cardNumber, userBudget);
                            ;

                        } else {
                            // الدفع عند الاستلام لا يتطلب إدخال أرقام
                            payment = new CashOnDelivery(userBudget);
                        }// خصم المبلغ والتأكد من الميزانية
                        payment.pay(order.calculateFinalTotal());
                        // 2. تنفيذ الدفع والتحقق من الحالة
                        if (!payment.getPaymentStatus()) {
                            System.out.println(
                                    "\n>>> Would you like to remove items from your cart to reduce the total? (1: Yes / 2: No)");
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
                        String receipt = ReceiptGenerator.generateReceipt(order, deliveryFee, totalPackagingFee,
                                packagingType);
                        System.out.println(receipt);
                        OrderRepository.saveReceiptText(receipt);

                        // -----------------------------------
                        DeliverySimulator simulator = new DeliverySimulator();
                        simulator.startLiveTracking(order);
                        // ==========================================
                        // خدمة العملاء والشكاوى بعد الاستلام
                        //==========================================
                        System.out.println("\nDo you have any issues or complaints about your order?");
                        System.out.println("1. No, everything is fine");
                        System.out.println("2. Yes, I want to submit a complaint");
                        int complaintCheck = readInt(scanner, "Select choice (1-2): ");

                        if (complaintCheck == 2) {
                            handleComplaintSection(scanner);
                        } else {
                            System.out.println("\nThank you for shopping with us! Have a great day.");
                        }
                        // ===========================================
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

                       // حفظ الطلب في قاعدة البيانات
                        orderHistory.add(order);
                        OrderRepository.saveOrders(orderHistory);
                        System.out.println("[DATABASE]: Order saved successfully!");

                        // الانتظار لقراءة الفاتورة قبل العودة للمنيو
                        System.out.print("\nPress Enter to return to Main Menu...");
                        scanner.nextLine();

                    } catch (InvalidOrderException e) {
                        System.out.println("\n[ORDER ERROR]: " + e.getMessage());
                    }
                }

                case 2 -> {// معالجة طلبات الإرجاع برقم الطلب
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

                case 3 -> {// إنهاء البرنامج    
                    keepRunning = false;
                    System.out.println("\nThank you for using VELOX!");
                }

                default -> System.out.println("Invalid choice!");
            }
        }

        scanner.close();
    }
    // عرض المنتجات وإضافتها للسلة
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
    // دالة مساعدة لقراءة الأرقام الصحيحة والتحقق من صحة المدخلات
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
    // إدارة الشكاوى وتوليد كود التعويض
    public static void handleComplaintSection(Scanner scanner) {
        System.out.println("\n--- COMPLAINT & SUPPORT SYSTEM ---");
        System.out.print("Enter Order ID related to your complaint: ");
        String orderId = scanner.next();
        scanner.nextLine();

        System.out.print("Please describe your problem: ");
        String details = scanner.nextLine();

        String complaintId = "CMP-" + (int) (Math.random() * 9000 + 1000);
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
            String generatedPromo = "SORRY" + (int) (Math.random() * 9000 + 1000);
            activePromoCodes.add(generatedPromo);

            System.out.println("==========================================");
            System.out.println("? Compensation Discount Generated!");
            System.out.println("Use Code: " + generatedPromo + " on your next order for 10% OFF.");
            System.out.println("==========================================");
        } catch (InterruptedException e) {
            System.out.println("Tracking interrupted.");
        }
    }
    // فحص بيانات الطلب والسلة قبل الإتمام
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
    // التعديل على محتويات السلة والحذف منها
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
    // تحديد المنطقة الجغرافية بناءً على المدخلات
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
    // دالة توليد رقم تسلسلي فريد لكل طلب جديد
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

