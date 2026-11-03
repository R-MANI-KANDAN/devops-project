package com.licet.foodorder.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class User {
    public enum Role { ROLE_CUSTOMER, ROLE_VENDOR, ROLE_ADMIN }

    private Long id;
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Email is required")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    @NotBlank(message = "Phone number is required")
    private String phone;
    private Role role = Role.ROLE_CUSTOMER;
    private boolean active = true;

    public User() {}
    public User(Long id, String name, String email, String password, String phone, Role role, boolean active) {
        this.id = id; this.name = name; this.email = email; this.password = password; this.phone = phone; this.role = role; this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
