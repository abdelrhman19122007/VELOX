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
import com.app.payment.PaymentMethod;
import com.app.payment.WalletPayment;
import com.app.service.DeliverySimulator;
import com.app.service.ReturnService;
import com.app.service.CustomerAccountService;
import com.app.service.LoyaltyService;
import com.app.service.OfferService;
import com.app.service.OtpService;
import com.app.util.InvoicePdfExporter;
import java.nio.file.Path;
import com.app.util.OrderRepository;
import com.app.util.PromoCodeManager;
import com.app.util.ReceiptGenerator;
import com.app.util.Response;
import com.app.service.PackagingService;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;
import com.app.model.order.Review;
import com.app.enums.Size;
import com.app.model.product.*;
import com.app.enums.Governorate;
import com.app.enums.Zone;
import com.app.model.order.Cart;
import com.app.util.StoreRepository;
// نقطة بداية التشغيل الأساسية للبرنامج وتحميل البيانات المخزنة
public class Main {
    public static Set<String> activePromoCodes = ConcurrentHashMap.newKeySet();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
                    // تصفح وطلب بدون تسجيل — تسجيل الدخول مطلوب عند الدفع فقط
                    System.out.println("\n[NOTE]: You can browse and fill your cart as a guest.");
                    System.out.println("Login (or a new account) is required at checkout to pay.");
                    // عنوان التوصيل: اختيار المحافظة بالكود من الليستة
                    Governorate selectedGov = readGovernorate(scanner, "DELIVERY GOVERNORATE");
                    // إدخال الميزانية المتاحة مع العميل
                    double userBudget = readDouble(scanner, "Enter your available Budget (EGP): ");

                    // سلة ضيف مؤقتة (تتحول لطلب حقيقي مربوط بالحساب بعد الدخول عند الدفع)
                    Order order = new Order("GUEST-CART", selectedGov.name(), "01000000000");
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

                        // الدفع يتطلب تسجيل الدخول (أو حساب جديد لأول مرة)
                        Session session = requireLogin(scanner);
                        if (session == null) {
                            System.out.println("Checkout cancelled. Your cart was discarded.");
                            break;
                        }
                        String userKey = session.userKey;
                        String phone = session.phone;
                        String customerName = session.name;
                        System.out.println("\n[ACCOUNT]: Welcome " + customerName
                                + " (" + session.email + " | " + phone + " | " + session.governorate + ")");
                        System.out.println("[LOYALTY]: " + LoyaltyService.progressMessage(session.email));
                        System.out.println("--- OFFERS FOR YOU ---");
                        for (String offer : OfferService.personalizedOffers(
                                CustomerAccountService.loadUserOrders(userKey))) {
                            System.out.println("  * " + offer);
                        }

                        // تحويل سلة الضيف لطلب حقيقي مربوط بإيميل الحساب (للواجهة وسجل العميل)
                        Order finalOrder = new Order("ORD-" + getNextOrderId(orderHistory),
                                session.email, order.getCity(), phone);
                        finalOrder.setCustomerNameForDelivery(customerName);
                        finalOrder.getProducts().addAll(order.getProducts());
                        order = finalOrder;

                        Cart tempCart = new Cart();
                        for (Product p : order.getProducts()) {
                            tempCart.addProduct(p); // أو اسم دالة إضافة المنتج للسلة لديك
                        }
                        // سعر التوصيل = سعر شحن المحافظة + الوزن + رسوم النوع
                        double deliveryFee = tempCart.createDelivery(order.getCustomerName(), selectedGov).calculatePrice();
                        System.out.printf(java.util.Locale.US,
                                "[DELIVERY]: %s | Area: %.0f EGP | ~%d day(s) | Total w/ weight & handling: %.2f EGP%n",
                                selectedGov.name(), selectedGov.getShippingPrice(),
                                selectedGov.getDeliveryDays(), deliveryFee);
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

                        // مكافأة الولاء: كل 5 طلبات ناجحة = توصيل مجاني مرة
                        boolean loyaltyRewardApplied = false;
                        if (deliveryFee > 0 && LoyaltyService.isRewardAvailable(session.email)) {
                            deliveryFee = 0;
                            loyaltyRewardApplied = true;
                            System.out.println(">>> Loyalty reward applied: FREE delivery (5 successful orders)!");
                        }

                       // تحديد نوع التغليف وحساب التكلفة
                        double unitPackagingFee = 0.0;
                        String packagingType = "None";

                        System.out.println("\nSelect Packaging Type:");
                        System.out.println("1. Standard Packaging (10 EGP / item)");
                        System.out.println("2. Gift Packaging (25 EGP / item)");
                        System.out.println("3. No Packaging");

                        int packChoice = readInt(scanner, "Choice: ");

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
                        // تحديد طريقة الدفع: كاش عند الاستلام أو رصيد الأكونت فقط
                        System.out.println("1. Cash on Delivery");
                        System.out.println("2. Wallet / Account balance");
                        int payChoice = readInt(scanner, "Select Payment Method (1-2): ");

                        // 1. إنشاء كائن وسيلة الدفع بناءً على اختيار المستخدم
                        PaymentMethod payment = null;
                        String paymentError = null;

                        if (payChoice == 2) {
                            // Real wallet balance from MySQL (not the simulated budget).
                            // Cards top up the wallet (website/account page); checkout spends it.
                            com.app.dao.WebOrderDAO webUsers = new com.app.dao.WebOrderDAO();
                            Integer walletUserId = webUsers.findUserId(session.email);
                            double walletBalance = walletUserId == null ? -1
                                    : new com.app.dao.UserDAO().getBalance(walletUserId);
                            if (walletUserId == null) {
                                paymentError = "No database account found for wallet payment.";
                            } else {
                                System.out.printf(java.util.Locale.US,
                                        "[WALLET]: balance = %.2f EGP%n", walletBalance);
                                payment = new WalletPayment(phone, walletBalance);
                            }

                        } else if (payChoice == 1) {
                            // الدفع عند الاستلام لا يتطلب إدخال أرقام
                            payment = new CashOnDelivery(userBudget);
                        } else {
                            System.out.println("Invalid payment choice. Order cancelled.");
                            break;
                        }
                        if (paymentError != null) {
                            System.out.println("\n[PAYMENT ERROR]: " + paymentError);
                            System.out.println("Order cancelled.");
                            break;
                        }
                        // FIX: must pay FULL amount (products after discount + delivery + packaging)
                        // Old bug: payment.pay(order.calculateFinalTotal()) ignored fees
                        payment.pay(amountToPay);
                        boolean isPaymentSuccessful = payment.getPaymentStatus();
                        // 2. تنفيذ الدفع والتحقق من الحالة
                        if (!isPaymentSuccessful) {
                            if (payChoice == 2) {
                                System.out.println(
                                        "\n>>> Wallet balance insufficient. Top up your account first (website/account page),");
                                int w = readInt(scanner,
                                        ">>> or switch to Cash on Delivery? (1: Cash / 2: Edit cart / 3: Cancel): ");
                                if (w == 1) {
                                    payment = new CashOnDelivery(userBudget);
                                    payment.pay(amountToPay);
                                    isPaymentSuccessful = payment.getPaymentStatus();
                                    if (!isPaymentSuccessful) {
                                        System.out.println("Order cancelled due to payment failure.");
                                        break;
                                    }
                                } else if (w == 2) {
                                    manageCart(scanner, order);
                                    System.out.println("Please restart checkout to re-pay with updated cart. Order NOT completed.");
                                    break;
                                } else {
                                    System.out.println("Order cancelled due to payment failure.");
                                    break;
                                }
                            } else {
                                System.out.println(
                                        "\n>>> Would you like to remove items from your cart to reduce the total? (1: Yes / 2: No)");
                                int choice = readInt(scanner, "Choice (1-2): ");

                                if (choice == 1) {
                                    manageCart(scanner, order); // تحويله لواجهة حذف المنتجات من السلة
                                    System.out.println("Please restart checkout to re-pay with updated cart. Order NOT completed.");
                                    break;
                                } else {
                                    System.out.println("Order cancelled due to payment failure.");
                                    break;
                                }
                            }
                        }
                        if (payChoice == 2) {
                            // Persist the wallet debit in MySQL (atomic: fails if balance moved).
                            Integer walletUserId = new com.app.dao.WebOrderDAO().findUserId(session.email);
                            if (walletUserId == null
                                    || !new com.app.dao.UserDAO().debit(walletUserId, amountToPay)) {
                                System.out.println("[WALLET WARNING]: MySQL debit failed. Order kept as paid locally.");
                            } else {
                                System.out.println("[WALLET]: Debited from MySQL wallet.");
                            }
                        }
                        // Mirror to MySQL (single source of truth; receipt math stays console-side).
                        try {
                            com.app.dao.WebOrderDAO mirror = new com.app.dao.WebOrderDAO();
                            Integer mirrorUser = mirror.findUserId(session.email);
                            if (mirrorUser != null) {
                                java.util.Map<Integer, Integer> qty = new java.util.LinkedHashMap<>();
                                for (Product p : order.getProducts()) {
                                    try {
                                        int pid = Integer.parseInt(p.getId());
                                        qty.merge(pid, 1, Integer::sum);
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                                java.util.List<int[]> mirrorItems = new java.util.ArrayList<>();
                                for (java.util.Map.Entry<Integer, Integer> e : qty.entrySet()) {
                                    mirrorItems.add(new int[]{e.getKey(), e.getValue()});
                                }
                                double discount = order.calculateRawTotal() - order.calculateFinalTotal();
                                mirror.placeOrderWithTotals(order.getOrderId(), mirrorUser,
                                        order.getCity() + ", " + phone, order.getCity(),
                                        order.calculateRawTotal(), discount, deliveryFee, amountToPay,
                                        "PAID", mirrorItems);
                                System.out.println("[DATABASE]: Order mirrored to MySQL.");
                            }
                        } catch (Exception ex) {
                            System.out.println("[DATABASE WARNING]: MySQL mirror failed: " + ex.getMessage());
                        }
                        PackagingService.packageOrder(order);
                        order.setStatus(OrderStatus.PAID);
                        if (loyaltyRewardApplied) {
                            LoyaltyService.consumeReward(session.email);
                            CustomerAccountService.recordReward(userKey, order.getOrderId());
                        }

                        // 2. خطوة إنشاء وطباعة الفاتورة
                        String receipt = ReceiptGenerator.generateReceipt(order, deliveryFee, totalPackagingFee,
                                packagingType);
                        System.out.println(receipt);
                        OrderRepository.saveReceiptText(receipt);
                        try {
                            Path pdfPath = CustomerAccountService.invoicePdfPath(userKey, order.getOrderId());
                            InvoicePdfExporter.export(receipt, order.getOrderId(), pdfPath);
                            CustomerAccountService.recordPdf(userKey, order.getOrderId(), pdfPath);
                            System.out.println("[PDF]: Invoice saved to " + pdfPath);
                        } catch (Exception ex) {
                            System.out.println("[PDF WARNING]: Could not export PDF: " + ex.getMessage());
                        }

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
                            handleComplaintSection(scanner, userKey);
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
                        CustomerAccountService.recordCompletedOrder(order);
                        System.out.println("[DATABASE]: Order saved successfully!");
                        System.out.println("[LOYALTY]: " + LoyaltyService.progressMessage(session.email));

                        // الانتظار لقراءة الفاتورة قبل العودة للمنيو
                        System.out.print("\nPress Enter to return to Main Menu...");
                        scanner.nextLine();

                    } catch (InvalidOrderException e) {
                        System.out.println("\n[ORDER ERROR]: " + e.getMessage());
                    }
                }

                case 2 -> {// معالجة طلبات الإرجاع برقم الطلب (تسجيل دخول + ملكية)
                    System.out.println("\n=== RETURN REQUEST ===");
                    Session session = requireLogin(scanner);
                    if (session == null) {
                        break;
                    }
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
                    } else if (!isOwner(foundOrder, session)) {
                        System.out.println("[DENIED] This order belongs to another account.");
                    } else {
                        try {
                            Response<Double> returnResponse = returnService.processReturn(foundOrder);
                            OrderRepository.saveOrders(orderHistory);
                            CustomerAccountService.recordReturn(foundOrder);
                            new com.app.dao.WebOrderDAO().markReturnedByCode(foundOrder.getOrderId());
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
            int quantity = readInt(scanner, "Enter quantity (1-50): ");

            if (quantity >= 1 && quantity <= 50) {
                order.addProduct(products.get(choice - 1), quantity);
                System.out.println("\n[SUCCESS] Item added to cart successfully!");
            } else {
                System.out.println("\n[ERROR] Quantity must be between 1 and 50.");
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

    private static double readDouble(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                double v = Double.parseDouble(input);
                if (v < 0) {
                    System.out.println("Value cannot be negative.");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid amount.");
            }
        }
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        return phone.trim().matches("01\\d{9}");
    }

    /** يعرض ليستة المحافظات بالأكواد ويقرأ كود أو اسم حتى يدخل قيمة صحيحة. */
    public static Governorate readGovernorate(Scanner scanner, String title) {
        System.out.println("\n--- " + title + " (enter CODE 1-27 or name) ---");
        for (Governorate g : Governorate.values()) {
            System.out.println(Governorate.menuLine(g));
        }
        while (true) {
            System.out.print("Choose governorate: ");
            String input = scanner.nextLine().trim();
            try {
                return Governorate.fromCodeOrName(input);
            } catch (IllegalArgumentException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
    }

    /** Logged-in customer carried through one menu operation. */
    private static class Session {
        final String email;
        final String userKey;
        final String name;
        final String phone;
        final String governorate;

        Session(CustomerAccountService.Profile p) {
            this.email = p.email;
            this.userKey = CustomerAccountService.keyForEmail(p.email);
            this.name = p.name;
            this.phone = p.phone;
            this.governorate = p.governorate;
        }
    }

    /** Login-or-register gate: returns null when the user aborts/fails. */
    private static Session requireLogin(Scanner scanner) {
        System.out.println("\n=== ACCOUNT LOGIN (required) ===");
        System.out.println("1. Login with email + password");
        System.out.println("2. Register new account");
        int choice = readInt(scanner, "Choose (1-2): ");
        if (choice == 1) {
            return doLogin(scanner);
        } else if (choice == 2) {
            return doRegister(scanner);
        }
        System.out.println("Cancelled.");
        return null;
    }

    private static Session doLogin(Scanner scanner) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine();
            try {
                return new Session(CustomerAccountService.login(email, password));
            } catch (IllegalArgumentException e) {
                System.out.println("[LOGIN FAILED]: " + e.getMessage()
                        + " (attempt " + attempt + "/3)");
            }
        }
        System.out.println("Too many failed attempts. Back to menu.");
        return null;
    }

    private static Session doRegister(Scanner scanner) {
        System.out.println("\n--- NEW ACCOUNT ---");
        System.out.print("Full name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Phone (11 digits, e.g. 01xxxxxxxxx): ");
        String phone = scanner.nextLine().trim();
        Governorate regGov = readGovernorate(scanner, "YOUR GOVERNORATE");
        String governorate = regGov.name();
        System.out.print("Password (min 8 chars): ");
        String pw1 = scanner.nextLine();
        System.out.print("Confirm password: ");
        String pw2 = scanner.nextLine();
        if (!pw1.equals(pw2)) {
            System.out.println("[REGISTER FAILED]: Passwords do not match.");
            return null;
        }
        CustomerAccountService.Profile pending;
        try {
            pending = CustomerAccountService.register(email, pw1, name, phone, governorate);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("[REGISTER FAILED]: " + e.getMessage());
            return null;
        }
        // OTP verification (dev-mode: code shown on screen; production sends SMS).
        String otp = OtpService.issue(pending.email);
        System.out.println("[OTP]: Your verification code (dev-mode shown here): " + otp);
        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.print("Enter OTP code: ");
            String code = scanner.nextLine().trim();
            try {
                CustomerAccountService.Profile p = CustomerAccountService.confirmRegistration(pending.email, code);
                System.out.println("[REGISTERED]: Welcome " + p.name + "!");
                return new Session(p);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("[OTP FAILED]: " + e.getMessage() + " (attempt " + attempt + "/3)");
            }
        }
        System.out.println("Verification failed. Your account stays inactive until verified.");
        return null;
    }

    /** An order belongs to the session when email or phone matches (legacy compatible). */
    private static boolean isOwner(Order order, Session s) {
        if (order == null || s == null) {
            return false;
        }
        String uid = order.getUserId();
        if (uid != null && (uid.equalsIgnoreCase(s.email) || uid.equals(s.phone))) {
            return true;
        }
        String ph = order.getPhone();
        return ph != null && ph.replaceAll("[^0-9]", "").equals(s.phone.replaceAll("[^0-9]", ""));
    }
    // إدارة الشكاوى وتوليد كود التعويض
    public static void handleComplaintSection(Scanner scanner, String userKey) {
        System.out.println("\n--- COMPLAINT & SUPPORT SYSTEM ---");
        System.out.print("Enter Order ID related to your complaint: ");
        String orderId = scanner.nextLine().trim();
        if (orderId.isEmpty()) {
            System.out.println("[ERROR] Order ID is required.");
            return;
        }

        System.out.print("Please describe your problem: ");
        String details = scanner.nextLine().trim();
        if (details.isEmpty()) {
            System.out.println("[ERROR] Description is required.");
            return;
        }

        String complaintId = "CMP-" + (1000 + SECURE_RANDOM.nextInt(9000));
        com.app.model.order.Complaint complaint = new com.app.model.order.Complaint(complaintId, orderId, details);

        com.app.util.ComplaintRepository.saveComplaintToFile(complaint);
        if (userKey != null && !userKey.isBlank()) {
            CustomerAccountService.recordComplaint(userKey, complaintId, orderId);
        }

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
            String generatedPromo = "SORRY" + (1000 + SECURE_RANDOM.nextInt(9000));
            activePromoCodes.add(generatedPromo);

            System.out.println("==========================================");
            System.out.println("Compensation Discount Generated!");
            System.out.println("Use Code: " + generatedPromo + " on your next order for 15% OFF (Seasonal).");
            System.out.println("==========================================");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Tracking interrupted.");
        }
    }
    // فحص بيانات الطلب والسلة قبل الإتمام
    private static void validateOrder(Order order) throws InvalidOrderException {
        if (order == null || order.getProducts().isEmpty()) {
            throw new InvalidOrderException("Cannot process checkout: Order cart is empty.");
        }
        if (order.getCity() == null || order.getCity().trim().isEmpty()) {
            throw new InvalidOrderException("Cannot process checkout: Delivery city is missing.");
        }
        // المدينة المقبولة: Zone قديمة أو أي محافظة من الـ 27
        boolean areaOk = false;
        try {
            parseZoneStrict(order.getCity());
            areaOk = true;
        } catch (IllegalArgumentException ignored) {
            try {
                Governorate.fromName(order.getCity());
                areaOk = true;
            } catch (IllegalArgumentException ignored2) {
                areaOk = false;
            }
        }
        if (!areaOk) {
            throw new InvalidOrderException("Cannot process checkout: Unsupported delivery area '"
                    + order.getCity() + "'.");
        }
        if (order.getAddress().getPhone() == null || order.getAddress().getPhone().trim().isEmpty()) {
            throw new InvalidOrderException("Cannot process checkout: Phone number is missing.");
        }
        if (!isValidPhone(order.getAddress().getPhone())) {
            throw new InvalidOrderException("Cannot process checkout: Invalid phone number (expected 01xxxxxxxxx).");
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
    // FIX: exact match only. Old prefix logic mapped "A"/"London"/"L" to wrong zones.
    public static Zone parseZone(String input) {
        try {
            return parseZoneStrict(input);
        } catch (IllegalArgumentException ex) {
            System.out.println("[WARNING] Unknown city '" + input + "'. Defaulting to CAIRO.");
            return Zone.CAIRO;
        }
    }

    public static Zone parseZoneStrict(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery city is missing.");
        }
        String clean = input.trim().toUpperCase();
        for (Zone z : Zone.values()) {
            if (z.name().equalsIgnoreCase(clean)) {
                return z;
            }
        }
        throw new IllegalArgumentException("Unsupported city '" + input + "'. Use CAIRO/GIZA/ALEXANDRIA/DAMIETTA.");
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

