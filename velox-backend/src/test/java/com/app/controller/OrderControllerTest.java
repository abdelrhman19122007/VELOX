package com.app.controller;

import com.app.dao.WebOrderDAO;
import com.app.dto.OrderHistoryDto;
import com.app.dto.OrderTrackingDto;
import com.app.dto.PagedResponse;
import com.app.enums.OrderStatus;
import com.app.service.AuthTokenStore;
import com.app.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * KAN-126: OrderController tests with a mocked OrderService (Mockito).
 * Auth + ownership are exercised through a mocked token store.
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private static final String TOKEN = "Bearer test-token";
    private static final String EMAIL = "user@velox.test";

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService)).build();
    }

    private OrderTrackingDto trackingDto() {
        List<OrderTrackingDto.TrackingStep> timeline = List.of(
                new OrderTrackingDto.TrackingStep(OrderStatus.PENDING, true),
                new OrderTrackingDto.TrackingStep(OrderStatus.PROCESSING, true),
                new OrderTrackingDto.TrackingStep(OrderStatus.IN_TRANSIT, false),
                new OrderTrackingDto.TrackingStep(OrderStatus.ARRIVED, false),
                new OrderTrackingDto.TrackingStep(OrderStatus.DELIVERED, false));
        return new OrderTrackingDto("LX-1", OrderStatus.PROCESSING,
                LocalDateTime.of(2026, 9, 14, 10, 0), timeline, null);
    }

    @Test
    void historyWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/orders/history").param("userId", EMAIL))
                .andExpect(status().isForbidden());
    }

    @Test
    void historyReturnsPagedContentForOwner() throws Exception {
        OrderHistoryDto item = new OrderHistoryDto("LX-1", EMAIL, OrderStatus.DELIVERED,
                LocalDateTime.of(2026, 9, 14, 10, 0), 2, 150.0, "Cairo");
        when(orderService.getOrdersByUserId(EMAIL, 0, 10))
                .thenReturn(new PagedResponse<>(List.of(item), 0, 10, 1, 1));

        try (MockedStatic<AuthTokenStore> auth = Mockito.mockStatic(AuthTokenStore.class);
             MockedConstruction<WebOrderDAO> dao = Mockito.mockConstruction(WebOrderDAO.class,
                     (mock, ctx) -> when(mock.findUserId(EMAIL)).thenReturn(3))) {
            auth.when(() -> AuthTokenStore.resolve(TOKEN)).thenReturn(EMAIL);

            mockMvc.perform(get("/api/orders/history")
                            .header("Authorization", TOKEN)
                            .param("userId", EMAIL)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].orderId", is("LX-1")))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }
    }

    @Test
    void historyForAnotherUserIsForbidden() throws Exception {
        try (MockedStatic<AuthTokenStore> auth = Mockito.mockStatic(AuthTokenStore.class);
             MockedConstruction<WebOrderDAO> dao = Mockito.mockConstruction(WebOrderDAO.class,
                     (mock, ctx) -> when(mock.findUserId(EMAIL)).thenReturn(3))) {
            auth.when(() -> AuthTokenStore.resolve(TOKEN)).thenReturn(EMAIL);

            mockMvc.perform(get("/api/orders/history")
                            .header("Authorization", TOKEN)
                            .param("userId", "stranger@velox.test"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void trackingWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/orders/LX-1/tracking"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trackingReturnsTimelineForOwner() throws Exception {
        when(orderService.getTracking("LX-1")).thenReturn(trackingDto());
        when(orderService.ownsOrder("LX-1", EMAIL)).thenReturn(true);

        try (MockedStatic<AuthTokenStore> auth = Mockito.mockStatic(AuthTokenStore.class)) {
            auth.when(() -> AuthTokenStore.resolve(TOKEN)).thenReturn(EMAIL);

            mockMvc.perform(get("/api/orders/LX-1/tracking")
                            .header("Authorization", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId", is("LX-1")))
                    .andExpect(jsonPath("$.currentStatus", is("PROCESSING")))
                    .andExpect(jsonPath("$.timeline", hasSize(5)));
        }
    }

    @Test
    void trackingUnknownOrderIsNotFound() throws Exception {
        when(orderService.getTracking("NOPE"))
                .thenThrow(new IllegalArgumentException("Order not found: NOPE"));

        try (MockedStatic<AuthTokenStore> auth = Mockito.mockStatic(AuthTokenStore.class)) {
            auth.when(() -> AuthTokenStore.resolve(TOKEN)).thenReturn(EMAIL);

            mockMvc.perform(get("/api/orders/NOPE/tracking")
                            .header("Authorization", TOKEN))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void trackingForeignOrderIsForbidden() throws Exception {
        when(orderService.getTracking("LX-9")).thenReturn(trackingDto());
        when(orderService.ownsOrder("LX-9", EMAIL)).thenReturn(false);

        try (MockedStatic<AuthTokenStore> auth = Mockito.mockStatic(AuthTokenStore.class)) {
            auth.when(() -> AuthTokenStore.resolve(TOKEN)).thenReturn(EMAIL);

            mockMvc.perform(get("/api/orders/LX-9/tracking")
                            .header("Authorization", TOKEN))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void updateStatusAdvancesTracking() throws Exception {
        when(orderService.getTracking("LX-1")).thenReturn(trackingDto());
        when(orderService.ownsOrder("LX-1", EMAIL)).thenReturn(true);
        when(orderService.updateStatus("LX-1", OrderStatus.IN_TRANSIT)).thenReturn(trackingDto());

        try (MockedStatic<AuthTokenStore> auth = Mockito.mockStatic(AuthTokenStore.class)) {
            auth.when(() -> AuthTokenStore.resolve(TOKEN)).thenReturn(EMAIL);

            mockMvc.perform(patch("/api/orders/LX-1/status")
                            .header("Authorization", TOKEN)
                            .param("status", "IN_TRANSIT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId", is("LX-1")));
        }
    }

    @Test
    void updateStatusWithIllegalJumpIsBadRequest() throws Exception {
        when(orderService.getTracking("LX-1")).thenReturn(trackingDto());
        when(orderService.ownsOrder("LX-1", EMAIL)).thenReturn(true);
        when(orderService.updateStatus(anyString(), any()))
                .thenThrow(new IllegalStateException("Invalid transition"));

        try (MockedStatic<AuthTokenStore> auth = Mockito.mockStatic(AuthTokenStore.class)) {
            auth.when(() -> AuthTokenStore.resolve(TOKEN)).thenReturn(EMAIL);

            mockMvc.perform(patch("/api/orders/LX-1/status")
                            .header("Authorization", TOKEN)
                            .param("status", "DELIVERED"))
                    .andExpect(status().isBadRequest());
        }
    }
}
