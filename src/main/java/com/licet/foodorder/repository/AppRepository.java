package com.licet.foodorder.repository;

import com.licet.foodorder.model.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class AppRepository {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<Long, FoodItem> foodItems = new ConcurrentHashMap<>();
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong userSeq = new AtomicLong(1);
    private final AtomicLong itemSeq = new AtomicLong(1);
    private final AtomicLong orderSeq = new AtomicLong(1);

    // --- Users ---
    public User saveUser(User u) {
        if (u.getId() == null) u.setId(userSeq.getAndIncrement());
        users.put(u.getId(), u);
        return u;
    }
    public Optional<User> findUserByEmail(String email) {
        return users.values().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    }
    public boolean existsUserByEmail(String email) {
        return users.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }
    public List<User> findAllUsers() { return new ArrayList<>(users.values()); }
    public void deleteUser(Long id) { users.remove(id); }
    public long countUsers() { return users.size(); }

    // --- Food Items ---
    public FoodItem saveItem(FoodItem item) {
        if (item.getId() == null) item.setId(itemSeq.getAndIncrement());
        foodItems.put(item.getId(), item);
        return item;
    }
    public Optional<FoodItem> findItemById(Long id) { return Optional.ofNullable(foodItems.get(id)); }
    public List<FoodItem> findAllItems() { return new ArrayList<>(foodItems.values()); }
    public List<String> findCategories() {
        return foodItems.values().stream().map(FoodItem::getCategory).distinct().sorted().toList();
    }
    public List<FoodItem> searchItems(String query, String category, boolean availableOnly) {
        return foodItems.values().stream()
                .filter(i -> !availableOnly || i.isAvailable())
                .filter(i -> category == null || category.isBlank() || category.equalsIgnoreCase("ALL") || category.equalsIgnoreCase(i.getCategory()))
                .filter(i -> query == null || query.isBlank() || i.getName().toLowerCase().contains(query.toLowerCase()) || i.getDescription().toLowerCase().contains(query.toLowerCase()))
                .sorted(Comparator.comparing(FoodItem::getId)).toList();
    }
    public void deleteItem(Long id) { foodItems.remove(id); }
    public long countItems() { return foodItems.size(); }

    // --- Orders ---
    public Order saveOrder(Order o) {
        if (o.getId() == null) {
            o.setId(orderSeq.getAndIncrement());
            if (o.getOrderNumber() == null) o.setOrderNumber(String.format("ORD-%05d", o.getId()));
        }
        orders.put(o.getId(), o);
        return o;
    }
    public Optional<Order> findOrderById(Long id) { return Optional.ofNullable(orders.get(id)); }
    public List<Order> findAllOrders() {
        return orders.values().stream().sorted(Comparator.comparing(Order::getId).reversed()).toList();
    }
    public List<Order> findOrdersByCustomer(Long customerId) {
        return orders.values().stream().filter(o -> Objects.equals(o.getCustomerId(), customerId)).sorted(Comparator.comparing(Order::getId).reversed()).toList();
    }
    public List<Order> findOrdersByStatus(Order.OrderStatus status) {
        return orders.values().stream().filter(o -> o.getOrderStatus() == status).sorted(Comparator.comparing(Order::getId).reversed()).toList();
    }
    public List<Order> findOrdersByDate(LocalDate date) {
        return orders.values().stream().filter(o -> date.equals(o.getPickupDate())).sorted(Comparator.comparing(Order::getId).reversed()).toList();
    }
    public long countOrders() { return orders.size(); }
    public long countOrdersByStatus(Order.OrderStatus status) {
        return orders.values().stream().filter(o -> o.getOrderStatus() == status).count();
    }
    public BigDecimal calculateRevenue() {
        return orders.values().stream()
                .filter(o -> o.getOrderStatus() != Order.OrderStatus.CANCELLED && o.getOrderStatus() != Order.OrderStatus.REJECTED)
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public Map<String, Integer> getPopularItems() {
        Map<String, Integer> counts = new HashMap<>();
        orders.values().stream()
                .filter(o -> o.getOrderStatus() != Order.OrderStatus.CANCELLED && o.getOrderStatus() != Order.OrderStatus.REJECTED)
                .flatMap(o -> o.getItems().stream())
                .forEach(i -> counts.put(i.getFoodItemName(), counts.getOrDefault(i.getFoodItemName(), 0) + i.getQuantity()));
        return counts;
    }
}
