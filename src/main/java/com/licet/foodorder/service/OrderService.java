package com.licet.foodorder.service;

import com.licet.foodorder.model.*;
import com.licet.foodorder.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createPreOrder(User customer, List<CartItem> cartItems, LocalDate pickupDate,
                               String pickupTime, String paymentMethod, PaymentStatus paymentStatus, String notes) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty when placing an order.");
        }
        if (pickupDate == null) {
            throw new IllegalArgumentException("Please select a valid pickup date.");
        }
        if (pickupDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Pickup date cannot be in the past.");
        }
        if (pickupTime == null || pickupTime.isBlank()) {
            throw new IllegalArgumentException("Please select a preferred pickup time.");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        long itemIdSeq = 1;

        for (CartItem ci : cartItems) {
            OrderItem oi = new OrderItem(
                    itemIdSeq++,
                    ci.getFoodItem().getId(),
                    ci.getFoodItem().getName(),
                    ci.getFoodItem().getPrice(),
                    ci.getQuantity()
            );
            orderItems.add(oi);
            total = total.add(oi.getSubtotal());
        }

        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setCustomerName(customer.getName());
        order.setCustomerEmail(customer.getEmail());
        order.setCustomerPhone(customer.getPhone());
        order.setPickupDate(pickupDate);
        order.setPickupTime(pickupTime);
        order.setItems(orderItems);
        order.setTotalAmount(total);
        order.setPaymentStatus(paymentStatus != null ? paymentStatus : PaymentStatus.PENDING);
        order.setPaymentMethod(paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "Counter Payment");
        order.setOrderStatus(OrderStatus.PENDING);
        order.setNotes(notes);

        return orderRepository.save(order);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> findOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> findOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        order.setOrderStatus(newStatus);
        if (newStatus == OrderStatus.COMPLETED) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        return orderRepository.save(order);
    }

    public Order updatePaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        order.setPaymentStatus(paymentStatus);
        return orderRepository.save(order);
    }

    public void cancelOrderByCustomer(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        if (!Objects.equals(order.getCustomerId(), customerId)) {
            throw new SecurityException("Unauthorized action on order: " + orderId);
        }
        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order cannot be cancelled as it is already " + order.getOrderStatus());
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    // Reporting utilities
    public long getTotalOrders() {
        return orderRepository.count();
    }

    public long getCountByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    public BigDecimal getTotalRevenue() {
        return orderRepository.calculateTotalSales();
    }

    public Map<String, Integer> getPopularItems(int limit) {
        Map<String, Integer> map = orderRepository.getPopularFoodItems();
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public List<Order> getDailyOrders(LocalDate date) {
        return orderRepository.findByPickupDate(date);
    }
}
