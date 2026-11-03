package com.licet.foodorder;

import com.licet.foodorder.model.*;
import com.licet.foodorder.service.FoodItemService;
import com.licet.foodorder.service.OrderService;
import com.licet.foodorder.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FoodPreorderingApplicationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private FoodItemService foodItemService;

    @Autowired
    private OrderService orderService;

    @Test
    void contextLoads() {
        assertNotNull(userService);
        assertNotNull(foodItemService);
        assertNotNull(orderService);
    }

    @Test
    void testPreloadedUsers() {
        assertTrue(userService.findByEmail("admin@foodhub.com").isPresent());
        assertTrue(userService.findByEmail("vendor@foodhub.com").isPresent());
        assertTrue(userService.findByEmail("customer@foodhub.com").isPresent());
    }

    @Test
    void testFoodItemsAndSearch() {
        List<FoodItem> available = foodItemService.findAvailable();
        assertFalse(available.isEmpty());

        List<FoodItem> biryanis = foodItemService.searchItems("Biryani", "ALL", true);
        assertFalse(biryanis.isEmpty());
    }

    @Test
    void testOrderCreationAndStatusTransition() {
        User customer = userService.findByEmail("customer@foodhub.com").orElseThrow();
        FoodItem item = foodItemService.findAvailable().get(0);

        CartItem cartItem = new CartItem(item, 2);
        Order order = orderService.createPreOrder(
                customer,
                Collections.singletonList(cartItem),
                LocalDate.now().plusDays(1),
                "12:30 PM",
                "Counter Payment",
                PaymentStatus.PENDING,
                "Extra spicy please"
        );

        assertNotNull(order.getId());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());

        // Vendor confirms
        Order confirmed = orderService.updateOrderStatus(order.getId(), OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, confirmed.getOrderStatus());

        // Vendor prepares
        Order preparing = orderService.updateOrderStatus(order.getId(), OrderStatus.PREPARING);
        assertEquals(OrderStatus.PREPARING, preparing.getOrderStatus());

        // Vendor completes
        Order completed = orderService.updateOrderStatus(order.getId(), OrderStatus.COMPLETED);
        assertEquals(OrderStatus.COMPLETED, completed.getOrderStatus());
        assertEquals(PaymentStatus.PAID, completed.getPaymentStatus());
    }
}
