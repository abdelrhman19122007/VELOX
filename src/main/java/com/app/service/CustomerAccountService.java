package com.app.service;

import com.app.enums.Governorate;
import com.app.enums.OrderStatus;
import com.app.model.order.Order;
import com.app.util.PasswordUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Per-customer accounts for VELOX (console + API share this store).
 * Every account is keyed by <b>email</b> and stores:
 * <ul>
 *   <li>email + phone + display name + governorate + password hash (SHA-256)</li>
 *   <li>{@code orders.dat} - his own orders (kept in sync on checkout/return)</li>
 *   <li>{@code transactions.log} - every event: register/login/completed/returned/...</li>
 *   <li>{@code loyalty.properties} - consumed free-delivery rewards</li>
 *   <li>{@code invoices/INV-&lt;orderId&gt;.pdf} - exported PDF invoices</li>
 * </ul>
 * Login is always by email + password, so the console and the frontend API
 * ({@code /api/auth/*}) authenticate the exact same accounts.
 * Base dir is {@code data/users} (overridable via {@code -Dvelox.data.dir=...} for tests).
 */
public final class CustomerAccountService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CustomerAccountService() {
    }

    /** Safe-to-return profile (never contains the password hash). */
    public static final class Profile {
        public final String email;
        public final String name;
        public final String phone;
        public final String governorate;

        public Profile(String email, String name, String phone, String governorate) {
            this.email = email;
            this.name = name;
            this.phone = phone;
            this.governorate = governorate;
        }
    }

    public static Path baseDir() {
        return Paths.get(System.getProperty("velox.data.dir", "data/users"));
    }

    /**
     * Normalized account key. Emails (contain '@') are the primary key;
     * bare phone numbers still resolve for backward compatibility.
     */
    public static String keyFor(String id) {
        if (id == null) {
            return "guest";
        }
        String t = id.trim();
        if (t.contains("@")) {
            return keyForEmail(t);
        }
        String digits = t.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? "guest" : digits;
    }

    public static String keyForEmail(String email) {
        String e = email.trim().toLowerCase();
        return "e_" + e.replaceAll("[^a-z0-9]", "_");
    }

    public static String keyForPhone(String phone) {
        if (phone == null) {
            return "guest";
        }
        String digits = phone.trim().replaceAll("[^0-9]", "");
        return digits.isEmpty() ? "guest" : digits;
    }

    public static Path userDir(String userKey) {
        return baseDir().resolve(userKey);
    }

    public static Path ordersFile(String userKey) {
        return userDir(userKey).resolve("orders.dat");
    }

    public static Path transactionsFile(String userKey) {
        return userDir(userKey).resolve("transactions.log");
    }

    public static Path profileFile(String userKey) {
        return userDir(userKey).resolve("profile.properties");
    }

    public static Path invoicePdfPath(String userKey, String orderId) {
        String safe = orderId == null ? "UNKNOWN" : orderId.replaceAll("[^A-Za-z0-9_-]", "_");
        return userDir(userKey).resolve("invoices").resolve("INV-" + safe + ".pdf");
    }

    // ---------------- registration + login ----------------

    /**
     * Creates a new account. Throws IllegalArgumentException on any invalid
     * or duplicate data (bad email/phone/governorate, weak password,
     * email or phone already registered).
     */
    public static Profile register(String email, String rawPassword, String name,
                                   String phone, String governorate) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        String cleanName = name == null ? "" : name.trim();
        String cleanPhone = phone == null ? "" : phone.trim();
        if (!PasswordUtil.isValidEmail(cleanEmail)) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        PasswordUtil.requireStrong(rawPassword);
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (!cleanPhone.matches("01\\d{9}")) {
            throw new IllegalArgumentException("Phone must be 11 digits starting with 01.");
        }
        String gov;
        try {
            gov = Governorate.fromName(governorate).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown governorate. Use e.g. CAIRO, GIZA, ALEXANDRIA, DAMIETTA.");
        }

        String key = keyForEmail(cleanEmail);
        if (Files.exists(profileFile(key))) {
            throw new IllegalArgumentException("This email is already registered. Please login.");
        }
        String phoneOwner = findEmailByPhone(cleanPhone);
        if (phoneOwner != null) {
            throw new IllegalArgumentException(
                    "This phone is already registered with " + phoneOwner + ". Please login.");
        }

        // Adopt a legacy phone-keyed folder (created before email accounts existed).
        adoptLegacyPhoneDir(key, cleanPhone);

        try {
            Files.createDirectories(userDir(key));
            Files.createDirectories(userDir(key).resolve("invoices"));
            Properties p = new Properties();
            p.setProperty("email", cleanEmail);
            p.setProperty("name", cleanName);
            p.setProperty("phone", cleanPhone);
            p.setProperty("governorate", gov);
            p.setProperty("passwordHash", PasswordUtil.sha256(rawPassword));
            p.setProperty("created", LocalDateTime.now().format(TS));
            try (var out = Files.newOutputStream(profileFile(key))) {
                p.store(out, "VELOX customer profile");
            }
            log(key, "ACCOUNT_REGISTERED", cleanEmail + " | " + cleanName + " | " + gov);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create account: " + e.getMessage());
        }
        return new Profile(cleanEmail, cleanName, cleanPhone, gov);
    }

    /**
     * Authenticates by email + password. Throws IllegalArgumentException
     * when the account is missing or the password is wrong (same message
     * for both, so emails can't be probed).
     */
    public static Profile login(String email, String rawPassword) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        if (!PasswordUtil.isValidEmail(cleanEmail)) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        Properties p = readProfile(keyForEmail(cleanEmail));
        if (p == null || !PasswordUtil.verify(rawPassword, p.getProperty("passwordHash", ""))) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        Profile profile = new Profile(cleanEmail,
                p.getProperty("name", ""),
                p.getProperty("phone", ""),
                p.getProperty("governorate", ""));
        log(keyForEmail(cleanEmail), "LOGIN", cleanEmail);
        return profile;
    }

    public static Profile profileOf(String userKey) {
        Properties p = readProfile(userKey);
        if (p == null || p.getProperty("email", "").isEmpty()) {
            return null;
        }
        return new Profile(p.getProperty("email", ""), p.getProperty("name", ""),
                p.getProperty("phone", ""), p.getProperty("governorate", ""));
    }

    // ---------------- orders / transactions ----------------

    /** Legacy entry point: ensures folders exist (used before email accounts). */
    public static void getOrCreate(String phone, String customerName) {
        String key = keyForPhone(phone);
        try {
            Files.createDirectories(userDir(key));
            Files.createDirectories(userDir(key).resolve("invoices"));
            Path profile = profileFile(key);
            if (!Files.exists(profile)) {
                Properties p = new Properties();
                p.setProperty("phone", phone == null ? "" : phone.trim());
                p.setProperty("name", customerName == null ? "" : customerName.trim());
                p.setProperty("created", LocalDateTime.now().format(TS));
                try (var out = Files.newOutputStream(profile)) {
                    p.store(out, "VELOX customer profile");
                }
                log(key, "ACCOUNT_CREATED", "name=" + customerName);
            }
        } catch (IOException e) {
            System.err.println("[Account] init failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Order> loadUserOrders(String userKey) {
        File file = ordersFile(userKey).toFile();
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
                    "com.app.model.order.*;com.app.model.product.*;com.app.model.store.*;com.app.enums.*;com.app.discount.*;java.util.*;java.time.*;java.lang.*;!*");
            ois.setObjectInputFilter(filter);
            Object obj = ois.readObject();
            if (obj instanceof List) {
                return new ArrayList<>((List<Order>) obj);
            }
        } catch (IOException | ClassNotFoundException | SecurityException e) {
            System.err.println("[Account] load orders failed: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static void saveUserOrders(String userKey, List<Order> orders) {
        try {
            Files.createDirectories(userDir(userKey));
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ordersFile(userKey).toFile()))) {
                oos.writeObject(new ArrayList<>(orders));
            }
        } catch (IOException e) {
            System.err.println("[Account] save orders failed: " + e.getMessage());
        }
    }

    /** Adds a new completed order (or replaces it if the ID already exists). */
    public static void recordCompletedOrder(Order order) {
        if (order == null) {
            return;
        }
        String key = accountKey(order);
        ensureProfileForOrder(order, key);
        List<Order> mine = loadUserOrders(key);
        mine.removeIf(o -> o.getOrderId().equalsIgnoreCase(order.getOrderId()));
        mine.add(order);
        saveUserOrders(key, mine);
        log(key, "ORDER_COMPLETED",
                order.getOrderId() + " total=" + String.format("%.2f", order.calculateFinalTotal())
                        + " status=" + order.getStatus());
    }

    /** Refreshes a mutated order (e.g. after a return) in the owner's file. */
    public static void recordReturn(Order order) {
        if (order == null) {
            return;
        }
        String key = accountKey(order);
        List<Order> mine = loadUserOrders(key);
        boolean found = false;
        for (int i = 0; i < mine.size(); i++) {
            if (mine.get(i).getOrderId().equalsIgnoreCase(order.getOrderId())) {
                mine.set(i, order);
                found = true;
            }
        }
        if (!found) {
            mine.add(order);
        }
        saveUserOrders(key, mine);
        log(key, "ORDER_RETURNED", order.getOrderId() + " status=" + order.getStatus());
    }

    public static void recordComplaint(String userKey, String complaintId, String orderId) {
        log(userKey, "COMPLAINT", complaintId + " order=" + orderId);
    }

    public static void recordReward(String userKey, String orderId) {
        log(userKey, "REWARD_CONSUMED", "free delivery on " + orderId);
    }

    public static void recordPdf(String userKey, String orderId, Path pdf) {
        log(userKey, "PDF_EXPORTED", orderId + " -> " + pdf);
    }

    public static void log(String userKey, String event, String details) {
        try {
            Files.createDirectories(userDir(userKey));
            try (FileWriter fw = new FileWriter(transactionsFile(userKey).toFile(), true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                out.println(LocalDateTime.now().format(TS) + " | " + event + " | " + (details == null ? "" : details));
            }
        } catch (IOException e) {
            System.err.println("[Account] log failed: " + e.getMessage());
        }
    }

    /** Counts successful (non-returned, delivered/paid) orders of a user. */
    public static int countSuccessful(List<Order> orders) {
        int n = 0;
        if (orders == null) {
            return 0;
        }
        for (Order o : orders) {
            if (o == null || o.isReturned() || o.getStatus() == OrderStatus.RETURNED) {
                continue;
            }
            if (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.PAID) {
                n++;
            }
        }
        return n;
    }

    // ---------------- internals ----------------

    private static String accountKey(Order order) {
        String uid = order.getUserId();
        if (uid != null && !uid.isBlank()) {
            return keyFor(uid);
        }
        return keyFor(order.getPhone());
    }

    private static void ensureProfileForOrder(Order order, String key) {
        if (Files.exists(profileFile(key))) {
            return;
        }
        String phone = order.getPhone() == null ? "" : order.getPhone();
        adoptLegacyPhoneDir(key, phone);
        if (!Files.exists(profileFile(key))) {
            Properties p = new Properties();
            p.setProperty("email", order.getUserId() == null ? "" : order.getUserId());
            p.setProperty("name", order.getCustomerName() == null ? "" : order.getCustomerName());
            p.setProperty("phone", phone);
            p.setProperty("governorate", "");
            p.setProperty("passwordHash", "");
            p.setProperty("created", LocalDateTime.now().format(TS));
            try (var out = Files.newOutputStream(profileFile(key))) {
                Files.createDirectories(userDir(key));
                p.store(out, "VELOX customer profile (auto)");
            } catch (IOException e) {
                System.err.println("[Account] auto-profile failed: " + e.getMessage());
            }
        }
    }

    private static Properties readProfile(String userKey) {
        Path f = profileFile(userKey);
        if (!Files.exists(f)) {
            return null;
        }
        try (var in = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            return p;
        } catch (IOException e) {
            return null;
        }
    }

    /** Finds the registered email owning a phone number (digits compared). */
    private static String findEmailByPhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        Path base = baseDir();
        if (!Files.isDirectory(base)) {
            return null;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(base)) {
            for (Path dir : ds) {
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                Properties p = readProfile(dir.getFileName().toString());
                if (p == null) {
                    continue;
                }
                String em = p.getProperty("email", "");
                String ph = p.getProperty("phone", "").replaceAll("[^0-9]", "");
                if (!em.isEmpty() && ph.equals(digits)) {
                    return em;
                }
            }
        } catch (IOException e) {
            // treat as not found
        }
        return null;
    }

    /** Moves a legacy phone-keyed folder (orders/log) under the new email key. */
    private static void adoptLegacyPhoneDir(String emailKey, String phone) {
        String phoneKey = keyForPhone(phone);
        if (phoneKey.equals(emailKey)) {
            return;
        }
        Path src = userDir(phoneKey);
        Path dst = userDir(emailKey);
        if (!Files.isDirectory(src) || Files.exists(profileFile(emailKey))) {
            return;
        }
        try {
            Files.createDirectories(dst.getParent());
            Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE);
            log(emailKey, "ACCOUNT_MIGRATED", "from phone folder " + phoneKey);
        } catch (IOException e) {
            // best effort: copy orders + log if atomic move unsupported
            try {
                Files.createDirectories(dst);
                Files.createDirectories(dst.resolve("invoices"));
                Path srcOrders = src.resolve("orders.dat");
                if (Files.exists(srcOrders)) {
                    Files.copy(srcOrders, dst.resolve("orders.dat"), StandardCopyOption.REPLACE_EXISTING);
                }
                Path srcLog = src.resolve("transactions.log");
                if (Files.exists(srcLog)) {
                    Files.copy(srcLog, dst.resolve("transactions.log"), StandardCopyOption.REPLACE_EXISTING);
                }
                log(emailKey, "ACCOUNT_MIGRATED", "copied from phone folder " + phoneKey);
            } catch (IOException ex) {
                System.err.println("[Account] migration failed: " + ex.getMessage());
            }
        }
    }
}
