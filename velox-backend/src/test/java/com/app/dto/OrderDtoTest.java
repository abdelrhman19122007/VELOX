package com.app.dto;

import com.app.enums.OrderStatus;
import com.app.model.order.Order;
import com.app.model.product.FoodItem;
import com.app.enums.Size;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KAN-126: DTO mapping tests (Order -> OrderHistoryDto, paged wrapper).
 */
class OrderDtoTest {

    @Test
    void historyDtoMapsOrderFields() {
        Order order = new Order("LX-99", "user@velox.test", "Cairo", "01000000001");
        order.addProduct(new FoodItem("F1", "Koshary", 120.0, "Large", Size.LARGE));
        order.addProduct(new FoodItem("F2", "Shawarma", 95.0, "Medium", Size.MEDIUM));
        order.setStatus(OrderStatus.PROCESSING);

        OrderHistoryDto dto = OrderHistoryDto.from(order);

        assertEquals("LX-99", dto.getOrderId());
        assertEquals("user@velox.test", dto.getUserId());
        assertEquals(OrderStatus.PROCESSING, dto.getStatus());
        assertEquals(2, dto.getItemCount());
        assertEquals(215.0, dto.getTotal());
        assertEquals("Cairo", dto.getCity());
        assertNotNull(dto.getOrderDate());
    }

    @Test
    void pagedResponseCarriesPagingMetadata() {
        OrderHistoryDto dto = new OrderHistoryDto("LX-1", "u", OrderStatus.PENDING,
                null, 0, 0.0, "Giza");
        PagedResponse<OrderHistoryDto> page =
                new PagedResponse<>(List.of(dto), 1, 10, 25, 3);

        assertEquals(1, page.getContent().size());
        assertEquals(1, page.getPage());
        assertEquals(10, page.getSize());
        assertEquals(25, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
    }

    @Test
    void trackingTimelineMarksReachedSteps() {
        List<OrderTrackingDto.TrackingStep> timeline = List.of(
                new OrderTrackingDto.TrackingStep(OrderStatus.PENDING, true),
                new OrderTrackingDto.TrackingStep(OrderStatus.PROCESSING, false));
        OrderTrackingDto dto = new OrderTrackingDto("LX-2", OrderStatus.PENDING,
                null, timeline, null);

        assertEquals("LX-2", dto.getOrderId());
        assertEquals(2, dto.getTimeline().size());
        assertTrue(dto.getTimeline().get(0).isReached());
        assertFalse(dto.getTimeline().get(1).isReached());
        assertNull(dto.getDriverLocation());
    }
}
