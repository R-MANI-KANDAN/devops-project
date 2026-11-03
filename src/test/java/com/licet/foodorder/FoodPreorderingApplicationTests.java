package com.licet.foodorder;

import com.licet.foodorder.model.*;
import com.licet.foodorder.model.Order.*;
import com.licet.foodorder.service.AppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FoodPreorderingApplicationTests {

    @Autowired
    private AppService service;

    @Test
    void contextLoads() {
        assertNotNull(service);
    }

    @Test
    void testPreloadedUsers() {
        assertTrue(service.findUserByEmail("admin@foodhub.com").isPresent());
        assertTrue(service.findUserByEmail("vendor@foodhub.com").isPresent());
        assertTrue(service.findUserByEmail("customer@foodhub.com").isPresent());
    }

    @Test
    void testFoodItemsAndSearch() {
        List<FoodItem> items = service.findAllItems();
        assertFalse(items.isEmpty());
        List<FoodItem> biryanis = service.searchItems("Biryani", "ALL", true);
        assertFalse(biryanis.isEmpty());
    }

    @Test
    void testPreOrderFlow() {
        User cust = service.findUserByEmail("customer@foodhub.com").orElseThrow();
        FoodItem item = service.findAllItems().get(0);

        CartItem cartItem = new CartItem(item, 2);
        Order order = service.placePreOrder(
                cust,
                List.of(cartItem),
                LocalDate.now().plusDays(1),
                "12:30 PM",
                "Counter Payment",
                PaymentStatus.PENDING,
                "Extra napkins"
        );

        assertNotNull(order.getId());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());

        // Status transitions
        Order confirmed = service.updateOrderStatus(order.getId(), OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, confirmed.getOrderStatus());

        Order completed = service.updateOrderStatus(order.getId(), OrderStatus.COMPLETED);
        assertEquals(OrderStatus.COMPLETED, completed.getOrderStatus());
        assertEquals(PaymentStatus.PAID, completed.getPaymentStatus());
    }
}
