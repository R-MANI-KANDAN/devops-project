package com.licet.foodorder.repository;

import com.licet.foodorder.model.Order;
import com.licet.foodorder.model.OrderItem;
import com.licet.foodorder.model.OrderStatus;
import com.licet.foodorder.model.PaymentStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class OrderRepository {
    private final Map<Long, Order> orderMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Order save(Order order) {
        if (order.getId() == null) {
            order.setId(idGenerator.getAndIncrement());
        }
        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            order.setOrderNumber(String.format("ORD-%05d", order.getId()));
        }
        orderMap.put(order.getId(), order);
        return order;
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orderMap.get(id));
    }

    public Optional<Order> findByOrderNumber(String orderNumber) {
        if (orderNumber == null) return Optional.empty();
        return orderMap.values().stream()
                .filter(o -> orderNumber.equalsIgnoreCase(o.getOrderNumber()))
                .findFirst();
    }

    public List<Order> findAll() {
        return orderMap.values().stream()
                .sorted(Comparator.comparing(Order::getId).reversed())
                .collect(Collectors.toList());
    }

    public List<Order> findByCustomerId(Long customerId) {
        return orderMap.values().stream()
                .filter(o -> Objects.equals(o.getCustomerId(), customerId))
                .sorted(Comparator.comparing(Order::getId).reversed())
                .collect(Collectors.toList());
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orderMap.values().stream()
                .filter(o -> o.getOrderStatus() == status)
                .sorted(Comparator.comparing(Order::getId).reversed())
                .collect(Collectors.toList());
    }

    public List<Order> findByPickupDate(LocalDate date) {
        return orderMap.values().stream()
                .filter(o -> date.equals(o.getPickupDate()))
                .sorted(Comparator.comparing(Order::getId).reversed())
                .collect(Collectors.toList());
    }

    public long count() {
        return orderMap.size();
    }

    public long countByStatus(OrderStatus status) {
        return orderMap.values().stream()
                .filter(o -> o.getOrderStatus() == status)
                .count();
    }

    public BigDecimal calculateTotalSales() {
        return orderMap.values().stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED && o.getOrderStatus() != OrderStatus.REJECTED)
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Integer> getPopularFoodItems() {
        Map<String, Integer> countMap = new HashMap<>();
        for (Order order : orderMap.values()) {
            if (order.getOrderStatus() != OrderStatus.CANCELLED && order.getOrderStatus() != OrderStatus.REJECTED) {
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        countMap.put(item.getFoodItemName(),
                                countMap.getOrDefault(item.getFoodItemName(), 0) + item.getQuantity());
                    }
                }
            }
        }
        return countMap;
    }

    public void deleteById(Long id) {
        orderMap.remove(id);
    }
}
