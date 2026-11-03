package com.licet.foodorder.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class FoodItem {
    private Long id;

    @NotBlank(message = "Food name is required")
    private String name;

    @NotBlank(message = "Category is required")
    private String category; // e.g., Starter, Main Course, Biryani, Beverage, Dessert

    @NotNull(message = "Price is required")
    @DecimalMin(value = "1.0", message = "Price must be at least 1.00")
    private BigDecimal price;

    @NotBlank(message = "Description is required")
    private String description;

    private boolean available = true;

    private String imageUrl;

    public FoodItem() {
    }

    public FoodItem(Long id, String name, String category, BigDecimal price, String description, boolean available, String imageUrl) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.available = available;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
