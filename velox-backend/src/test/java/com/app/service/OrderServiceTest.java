package com.app.service;

import com.app.dto.PagedResponse;
import com.app.enums.OrderStatus;
import com.app.util.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * KAN-126: OrderService unit tests with a mocked JDBC layer (Mockito
 * static mocking), so no live database is required.
 */
class OrderServiceTest {

    private MockedStatic<DatabaseConnection> db;
    private Connection conn;

    @BeforeEach
    void setUp() throws Exception {
        db = Mockito.mockStatic(DatabaseConnection.class);
        conn = mock(Connection.class);
        db.when(DatabaseConnection::getConnection).thenReturn(conn);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    /** Routes prepareStatement to the right stub by SQL content. */
    private void route(Answer<PreparedStatement> answer) throws Exception {
        when(conn.prepareStatement(anyString())).thenAnswer(answer);
    }

    private static ResultSet emptyRs() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(false);
        return rs;
    }

    @Test
    void historyRejectsBlankUserIdWithoutTouchingDb() throws Exception {
        OrderService service = new OrderService();
        assertThrows(IllegalArgumentException.class, () -> service.getOrdersByUserId("  ", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> service.getOrdersByUserId(null, 0, 10));
        verify(conn, never()).prepareStatement(anyString());
    }

    @Test
    void historyUnknownUserReturnsEmptyPage() throws Exception {
        ResultSet empty = emptyRs();
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeQuery()).thenReturn(empty);
        route(inv -> ps);

        PagedResponse<?> page = new OrderService().getOrdersByUserId("ghost@velox.test", 0, 10);

        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getTotalPages());
        assertTrue(page.getContent().isEmpty());
    }

    @Test
    void historyMapsRowsToDtos() throws Exception {
        ResultSet rsUser = mock(ResultSet.class);
        when(rsUser.next()).thenReturn(true, false);
        when(rsUser.getInt("id")).thenReturn(3);

        ResultSet rsCount = mock(ResultSet.class);
        when(rsCount.next()).thenReturn(true, false);
        when(rsCount.getInt(1)).thenReturn(1);

        ResultSet rsSel = mock(ResultSet.class);
        when(rsSel.next()).thenReturn(true, false);
        when(rsSel.getString("order_code")).thenReturn("LX-1");
        when(rsSel.getString("status")).thenReturn("DELIVERED");
        when(rsSel.getTimestamp("order_date"))
                .thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 9, 14, 10, 0)));
        when(rsSel.getInt("items")).thenReturn(2);
        when(rsSel.getDouble("final_amount")).thenReturn(150.0);
        when(rsSel.getString("shipping_address")).thenReturn("Cairo");

        route(inv -> {
            String sql = inv.getArgument(0);
            PreparedStatement ps = mock(PreparedStatement.class);
            if (sql.contains("FROM users WHERE")) {
                when(ps.executeQuery()).thenReturn(rsUser);
            } else if (sql.contains("COUNT(*)")) {
                when(ps.executeQuery()).thenReturn(rsCount);
            } else {
                when(ps.executeQuery()).thenReturn(rsSel);
            }
            return ps;
        });

        var page = new OrderService().getOrdersByUserId("user@velox.test", 0, 10);

        assertEquals(1, page.getTotalElements());
        assertEquals(1, page.getTotalPages());
        assertEquals(1, page.getContent().size());
        var dto = page.getContent().get(0);
        assertEquals("LX-1", dto.getOrderId());
        assertEquals(OrderStatus.DELIVERED, dto.getStatus());
        assertEquals(2, dto.getItemCount());
        assertEquals(150.0, dto.getTotal());
        assertEquals("Cairo", dto.getCity());
    }

    @Test
    void historyNormalizesOutOfRangePaging() throws Exception {
        ResultSet empty = emptyRs();
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeQuery()).thenReturn(empty);
        route(inv -> ps);

        var page = new OrderService().getOrdersByUserId("ghost@velox.test", -5, 500);

        assertEquals(0, page.getPage());
        assertEquals(10, page.getSize());
    }

    @Test
    void trackingUnknownOrderThrows() throws Exception {
        ResultSet empty = emptyRs();
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeQuery()).thenReturn(empty);
        route(inv -> ps);

        assertThrows(IllegalArgumentException.class,
                () -> new OrderService().getTracking("NOPE"));
    }

    @Test
    void updateStatusRejectsIllegalJump() throws Exception {
        ResultSet rsRow = mock(ResultSet.class);
        when(rsRow.next()).thenReturn(true, false);
        when(rsRow.getInt("id")).thenReturn(7);
        when(rsRow.getString("order_code")).thenReturn("LX-7");
        when(rsRow.getString("status")).thenReturn("PENDING");
        when(rsRow.getTimestamp("order_date"))
                .thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 9, 14, 9, 0)));

        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeQuery()).thenReturn(rsRow);
        route(inv -> ps);

        // PENDING -> DELIVERED skips steps: must fail before any UPDATE runs.
        assertThrows(IllegalStateException.class,
                () -> new OrderService().updateStatus("LX-7", OrderStatus.DELIVERED));
        verify(ps, never()).executeUpdate();
    }

    @Test
    void ownsOrderReturnsFalseForNullsWithoutDb() throws Exception {
        OrderService service = new OrderService();
        assertFalse(service.ownsOrder(null, "a@b.c"));
        assertFalse(service.ownsOrder("LX-1", null));
        verify(conn, never()).prepareStatement(anyString());
    }

    @Test
    void ownsOrderChecksMembership() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeQuery()).thenReturn(rs);
        route(inv -> ps);

        assertTrue(new OrderService().ownsOrder("LX-1", "user@velox.test"));
    }
}
