package com.licet.foodorder.service;

import com.licet.foodorder.model.*;
import com.licet.foodorder.model.Order.*;
import com.licet.foodorder.repository.AppRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppService {
    private final AppRepository repo;
    private final PasswordEncoder encoder;

    public AppService(AppRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    // --- Users ---
    public User registerUser(User u) {
        if (repo.existsUserByEmail(u.getEmail())) throw new IllegalArgumentException("Email already registered: " + u.getEmail());
        u.setPassword(encoder.encode(u.getPassword()));
        if (u.getRole() == null) u.setRole(User.Role.ROLE_CUSTOMER);
        u.setActive(true);
        return repo.saveUser(u);
    }
    public Optional<User> findUserByEmail(String email) { return repo.findUserByEmail(email); }
    public List<User> findAllUsers() { return repo.findAllUsers(); }
    public List<User> findUsersByRole(User.Role r) {
        return repo.findAllUsers().stream().filter(u -> u.getRole() == r).toList();
    }
    public void toggleUserActive(Long id) {
        repo.findAllUsers().stream().filter(u -> u.getId().equals(id)).findFirst().ifPresent(u -> {
            u.setActive(!u.isActive());
            repo.saveUser(u);
        });
    }
    public void deleteUser(Long id) { repo.deleteUser(id); }
    public long getTotalUsers() { return repo.countUsers(); }

    // --- Food Items ---
    public List<FoodItem> findAllItems() { return repo.findAllItems(); }
    public Optional<FoodItem> findItemById(Long id) { return repo.findItemById(id); }
    public List<String> getCategories() { return repo.findCategories(); }
    public List<FoodItem> searchItems(String q, String cat, boolean availOnly) {
        return repo.searchItems(q, cat, availOnly);
    }
    public FoodItem saveItem(FoodItem item) { return repo.saveItem(item); }
    public void toggleItemAvailability(Long id) {
        repo.findItemById(id).ifPresent(i -> { i.setAvailable(!i.isAvailable()); repo.saveItem(i); });
    }
    public void deleteItem(Long id) { repo.deleteItem(id); }
    public long getTotalItems() { return repo.countItems(); }

    // --- Orders ---
    public Order placePreOrder(User cust, List<CartItem> cart, LocalDate date, String time, String method, PaymentStatus payStatus, String notes) {
        if (cart == null || cart.isEmpty()) throw new IllegalArgumentException("Cart cannot be empty.");
        if (date == null || date.isBefore(LocalDate.now())) throw new IllegalArgumentException("Please choose a valid pickup date.");
        if (time == null || time.isBlank()) throw new IllegalArgumentException("Please choose a pickup time.");

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        long seq = 1;
        for (CartItem ci : cart) {
            OrderItem oi = new OrderItem(seq++, ci.getFoodItem().getId(), ci.getFoodItem().getName(), ci.getFoodItem().getPrice(), ci.getQuantity());
            items.add(oi);
            total = total.add(oi.getSubtotal());
        }

        Order o = new Order();
        o.setCustomerId(cust.getId());
        o.setCustomerName(cust.getName());
        o.setCustomerEmail(cust.getEmail());
        o.setCustomerPhone(cust.getPhone());
        o.setPickupDate(date);
        o.setPickupTime(time);
        o.setItems(items);
        o.setTotalAmount(total);
        o.setPaymentStatus(payStatus != null ? payStatus : PaymentStatus.PENDING);
        o.setPaymentMethod(method != null && !method.isBlank() ? method : "Counter Payment");
        o.setOrderStatus(OrderStatus.PENDING);
        o.setNotes(notes);
        return repo.saveOrder(o);
    }

    public List<Order> findAllOrders() { return repo.findAllOrders(); }
    public List<Order> findCustomerOrders(Long custId) { return repo.findOrdersByCustomer(custId); }
    public List<Order> findOrdersByStatus(OrderStatus s) { return repo.findOrdersByStatus(s); }
    public List<Order> findOrdersByDate(LocalDate d) { return repo.findOrdersByDate(d); }

    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order o = repo.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        o.setOrderStatus(status);
        if (status == OrderStatus.COMPLETED) o.setPaymentStatus(PaymentStatus.PAID);
        return repo.saveOrder(o);
    }

    public Order updatePaymentStatus(Long orderId, PaymentStatus status) {
        Order o = repo.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        o.setPaymentStatus(status);
        return repo.saveOrder(o);
    }

    public void cancelCustomerOrder(Long orderId, Long custId) {
        Order o = repo.findOrderById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (!Objects.equals(o.getCustomerId(), custId)) throw new SecurityException("Unauthorized");
        if (o.getOrderStatus() != OrderStatus.PENDING && o.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order cannot be cancelled in state: " + o.getOrderStatus());
        }
        o.setOrderStatus(OrderStatus.CANCELLED);
        repo.saveOrder(o);
    }

    // --- Analytics & Reports ---
    public long getTotalOrders() { return repo.countOrders(); }
    public long getCountByStatus(OrderStatus s) { return repo.countOrdersByStatus(s); }
    public BigDecimal getTotalRevenue() { return repo.calculateRevenue(); }
    public Map<String, Integer> getPopularItems(int limit) {
        return repo.getPopularItems().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }
}
