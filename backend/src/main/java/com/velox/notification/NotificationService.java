package com.velox.notification;

import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import com.velox.order.domain.Order;
import com.velox.order.domain.OrderStatus;
import com.velox.order.repository.OrderRepository;
import com.velox.order.spi.OrderChanged;
import com.velox.security.AuthUser;

/**
 * Tells people what happened to their orders (FR-PAY-10, -11): an in-app inbox and a push notification.
 * The store hears about new (paid) orders; the customer about every step. Sent only after the change is committed.
 */
@Service
public class NotificationService {

    public record Notification(long id, String title, String body, String orderCode, boolean read, Instant at) {
    }

    private final JdbcTemplate db;
    private final OrderRepository orders;
    private final PushService push;

    public NotificationService(JdbcTemplate db, OrderRepository orders, PushService push) {
        this.db = db;
        this.orders = orders;
        this.push = push;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onChange(OrderChanged change) {
        Order o = orders.findById(change.orderId()).orElse(null);
        if (o == null) {
            return;
        }
        if (change.to() == OrderStatus.PLACED && o.storeId() != null) {
            List<Long> staff = db.queryForList("SELECT id FROM users WHERE role = 'RESTAURANT' AND store_id = ?", Long.class, o.storeId());
            notify(staff, o, new PushService.Message("طلب جديد " + o.code(), "الإجمالي " + o.total() + " ج.م",
                    "New order " + o.code(), "Total EGP " + o.total(), o.code()));
        }
        PushService.Message forCustomer = switch (change.to()) {
            case ACCEPTED -> msg(o, "المطعم قبل طلبك", "بيتجهز دلوقتي", "Your order was accepted", "It's being prepared");
            case READY -> msg(o, "طلبك جاهز", "مستني المندوب", "Your order is ready", "Waiting for the driver");
            case PICKED_UP -> msg(o, "المندوب في الطريق", "طلبك جاي لك", "On the way", "The driver picked up your order");
            case DELIVERED -> msg(o, "طلبك وصل", "بالهنا — قيّم تجربتك", "Delivered", "Enjoy — rate your order");
            case REJECTED -> msg(o, "المطعم رفض طلبك", "لو دفعت أونلاين الفلوس رجعت محفظتك", "Order rejected", "Online payments go back to your wallet");
            case CANCELLED -> msg(o, "طلبك اتلغى", reason(o), "Order cancelled", reason(o));
            default -> null;
        };
        if (forCustomer != null) {
            notify(List.of(o.customerId()), o, forCustomer);
        }
    }

    public List<Notification> inbox(AuthUser me) {
        return db.query("SELECT n.id, n.title, n.body, o.code, n.read, n.created_at FROM notifications n "
                        + "LEFT JOIN orders o ON o.id = n.order_id WHERE n.user_id = ? ORDER BY n.id DESC LIMIT 50",
                (rs, i) -> new Notification(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBoolean(5),
                        rs.getTimestamp(6).toInstant()), me.id());
    }

    public void markAllRead(AuthUser me) {
        db.update("UPDATE notifications SET read = TRUE WHERE user_id = ? AND NOT read", me.id());
    }

    private void notify(List<Long> userIds, Order o, PushService.Message m) {
        for (Long id : userIds) {
            db.update("INSERT INTO notifications (user_id, title, body, order_id) VALUES (?,?,?,?)", id, m.titleAr(), m.bodyAr(), o.id());
        }
        push.send(userIds, m);
    }

    private static PushService.Message msg(Order o, String titleAr, String bodyAr, String titleEn, String bodyEn) {
        return new PushService.Message(titleAr + " · " + o.code(), bodyAr, titleEn + " · " + o.code(), bodyEn, o.code());
    }

    private static String reason(Order o) {
        return o.rejectReason() == null ? o.code() : o.rejectReason();
    }
}
