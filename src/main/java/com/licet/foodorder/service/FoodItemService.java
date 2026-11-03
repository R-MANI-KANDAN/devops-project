package com.licet.foodorder.service;

import com.licet.foodorder.model.FoodItem;
import com.licet.foodorder.repository.FoodItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FoodItemService {
    private final FoodItemRepository foodItemRepository;

    public FoodItemService(FoodItemRepository foodItemRepository) {
        this.foodItemRepository = foodItemRepository;
    }

    public List<FoodItem> findAll() {
        return foodItemRepository.findAll();
    }

    public List<FoodItem> findAvailable() {
        return foodItemRepository.findByAvailableTrue();
    }

    public Optional<FoodItem> findById(Long id) {
        return foodItemRepository.findById(id);
    }

    public List<String> getCategories() {
        return foodItemRepository.findAllCategories();
    }

    public List<FoodItem> searchItems(String query, String category, Boolean availableOnly) {
        return foodItemRepository.searchAndFilter(query, category, availableOnly);
    }

    public FoodItem saveOrUpdate(FoodItem foodItem) {
        return foodItemRepository.save(foodItem);
    }

    public void toggleAvailability(Long id) {
        foodItemRepository.findById(id).ifPresent(item -> {
            item.setAvailable(!item.isAvailable());
            foodItemRepository.save(item);
        });
    }

    public void delete(Long id) {
        foodItemRepository.deleteById(id);
    }

    public long getTotalCount() {
        return foodItemRepository.count();
    }
}
