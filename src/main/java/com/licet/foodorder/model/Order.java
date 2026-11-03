package com.licet.foodorder.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    public enum OrderStatus { PENDING, CONFIRMED, PREPARING, READY, COMPLETED, REJECTED, CANCELLED }
    public enum PaymentStatus { PENDING, PAID }

    private Long id;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDate pickupDate;
    private String pickupTime;
    private List<OrderItem> items = new ArrayList<>();
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    private String paymentMethod = "Counter Payment";
    private OrderStatus orderStatus = OrderStatus.PENDING;
    private String notes;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Order() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public LocalDate getPickupDate() { return pickupDate; }
    public void setPickupDate(LocalDate pickupDate) { this.pickupDate = pickupDate; }
    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public OrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class OrderItem {
        private Long id;
        private Long foodItemId;
        private String foodItemName;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal subtotal;

        public OrderItem() {}
        public OrderItem(Long id, Long foodItemId, String foodItemName, BigDecimal unitPrice, int quantity) {
            this.id = id; this.foodItemId = foodItemId; this.foodItemName = foodItemName;
            this.unitPrice = unitPrice; this.quantity = quantity;
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getFoodItemId() { return foodItemId; }
        public void setFoodItemId(Long foodItemId) { this.foodItemId = foodItemId; }
        public String getFoodItemName() { return foodItemName; }
        public void setFoodItemName(String foodItemName) { this.foodItemName = foodItemName; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    }

    public static class CartItem {
        private FoodItem foodItem;
        private int quantity;

        public CartItem() {}
        public CartItem(FoodItem foodItem, int quantity) {
            this.foodItem = foodItem; this.quantity = quantity;
        }

        public FoodItem getFoodItem() { return foodItem; }
        public void setFoodItem(FoodItem foodItem) { this.foodItem = foodItem; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getSubtotal() {
            return (foodItem != null && foodItem.getPrice() != null)
                    ? foodItem.getPrice().multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
        }
    }
}
