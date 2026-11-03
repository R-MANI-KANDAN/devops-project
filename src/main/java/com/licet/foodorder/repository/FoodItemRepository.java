package com.licet.foodorder.repository;

import com.licet.foodorder.model.FoodItem;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class FoodItemRepository {
    private final Map<Long, FoodItem> itemMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public FoodItem save(FoodItem item) {
        if (item.getId() == null) {
            item.setId(idGenerator.getAndIncrement());
        }
        itemMap.put(item.getId(), item);
        return item;
    }

    public Optional<FoodItem> findById(Long id) {
        return Optional.ofNullable(itemMap.get(id));
    }

    public List<FoodItem> findAll() {
        return new ArrayList<>(itemMap.values());
    }

    public List<FoodItem> findByAvailableTrue() {
        return itemMap.values().stream()
                .filter(FoodItem::isAvailable)
                .collect(Collectors.toList());
    }

    public List<String> findAllCategories() {
        return itemMap.values().stream()
                .map(FoodItem::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<FoodItem> searchAndFilter(String query, String category, Boolean availableOnly) {
        return itemMap.values().stream()
                .filter(item -> {
                    if (Boolean.TRUE.equals(availableOnly) && !item.isAvailable()) {
                        return false;
                    }
                    if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
                        if (!category.equalsIgnoreCase(item.getCategory())) {
                            return false;
                        }
                    }
                    if (query != null && !query.isBlank()) {
                        String lower = query.trim().toLowerCase();
                        boolean matchName = item.getName() != null && item.getName().toLowerCase().contains(lower);
                        boolean matchDesc = item.getDescription() != null && item.getDescription().toLowerCase().contains(lower);
                        boolean matchCat = item.getCategory() != null && item.getCategory().toLowerCase().contains(lower);
                        return matchName || matchDesc || matchCat;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(FoodItem::getId))
                .collect(Collectors.toList());
    }

    public void deleteById(Long id) {
        itemMap.remove(id);
    }

    public long count() {
        return itemMap.size();
    }
}
