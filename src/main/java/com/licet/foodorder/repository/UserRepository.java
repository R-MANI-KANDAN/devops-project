package com.licet.foodorder.repository;

import com.licet.foodorder.model.Role;
import com.licet.foodorder.model.User;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class UserRepository {
    private final Map<Long, User> userMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        userMap.put(user.getId(), user);
        return user;
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMap.get(id));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userMap.values().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst();
    }

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return userMap.values().stream()
                .anyMatch(u -> email.equalsIgnoreCase(u.getEmail()));
    }

    public List<User> findAll() {
        return new ArrayList<>(userMap.values());
    }

    public List<User> findByRole(Role role) {
        return userMap.values().stream()
                .filter(u -> u.getRole() == role)
                .collect(Collectors.toList());
    }

    public void deleteById(Long id) {
        userMap.remove(id);
    }

    public long count() {
        return userMap.size();
    }
}
