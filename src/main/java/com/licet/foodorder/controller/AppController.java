package com.licet.foodorder.controller;

import com.licet.foodorder.model.*;
import com.licet.foodorder.model.Order.*;
import com.licet.foodorder.service.AppService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
public class AppController {
    private final AppService service;

    public AppController(AppService service) { this.service = service; }

    @SuppressWarnings("unchecked")
    private List<CartItem> getCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) { cart = new ArrayList<>(); session.setAttribute("cart", cart); }
        return cart;
    }
    private int getCartCount(HttpSession s) { return getCart(s).stream().mapToInt(CartItem::getQuantity).sum(); }

    // --- Authentication ---
    @GetMapping("/") public String index(Authentication auth) { return auth != null ? "redirect:/home" : "redirect:/login"; }
    @GetMapping("/login")
    public String login(@RequestParam(value="error", required=false) String err,
                        @RequestParam(value="logout", required=false) String out,
                        @RequestParam(value="denied", required=false) String den, Model m) {
        if (err != null) m.addAttribute("errorMessage", "Invalid email or password.");
        if (out != null) m.addAttribute("successMessage", "Logged out successfully.");
        if (den != null) m.addAttribute("errorMessage", "Access Denied.");
        return "login";
    }
    @GetMapping("/register")
    public String registerForm(Model m) { if (!m.containsAttribute("user")) m.addAttribute("user", new User()); return "register"; }
    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("user") User user, BindingResult res, RedirectAttributes ra) {
        if (res.hasErrors()) return "register";
        try {
            service.registerUser(user);
            ra.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) { res.rejectValue("email", "error.user", e.getMessage()); return "register"; }
    }
    @GetMapping("/home")
    public String home(Authentication auth) {
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) return "redirect:/admin/dashboard";
        if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_VENDOR"))) return "redirect:/vendor/dashboard";
        return "redirect:/customer/menu";
    }

    // --- Customer Portal ---
    @GetMapping("/customer/menu")
    public String menu(@RequestParam(value="search", required=false) String q,
                       @RequestParam(value="category", defaultValue="ALL") String cat, Model m, HttpSession s) {
        m.addAttribute("foodItems", service.searchItems(q, cat, true));
        m.addAttribute("categories", service.getCategories());
        m.addAttribute("selectedCategory", cat);
        m.addAttribute("searchQuery", q != null ? q : "");
        m.addAttribute("cartCount", getCartCount(s));
        return "customer/menu";
    }
    @PostMapping("/customer/cart/add")
    public String addToCart(@RequestParam("foodId") Long id, @RequestParam(value="quantity", defaultValue="1") int qty, HttpSession s, RedirectAttributes ra) {
        service.findItemById(id).ifPresent(f -> {
            List<CartItem> cart = getCart(s);
            cart.stream().filter(c -> c.getFoodItem().getId().equals(id)).findFirst()
                    .ifPresentOrElse(c -> c.setQuantity(c.getQuantity() + qty), () -> cart.add(new CartItem(f, qty)));
            ra.addFlashAttribute("successMessage", "Added " + f.getName() + " to cart!");
        });
        return "redirect:/customer/menu";
    }
    @GetMapping("/customer/cart")
    public String viewCart(Model m, HttpSession s) {
        List<CartItem> cart = getCart(s);
        BigDecimal total = cart.stream().map(CartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        m.addAttribute("cartItems", cart);
        m.addAttribute("totalAmount", total);
        m.addAttribute("cartCount", getCartCount(s));
        m.addAttribute("todayDate", LocalDate.now().toString());
        m.addAttribute("maxDate", LocalDate.now().plusDays(7).toString());
        return "customer/cart";
    }
    @PostMapping("/customer/cart/update")
    public String updateCart(@RequestParam("foodId") Long id, @RequestParam("quantity") int qty, HttpSession s) {
        List<CartItem> cart = getCart(s);
        if (qty <= 0) cart.removeIf(c -> c.getFoodItem().getId().equals(id));
        else cart.stream().filter(c -> c.getFoodItem().getId().equals(id)).findFirst().ifPresent(c -> c.setQuantity(qty));
        return "redirect:/customer/cart";
    }
    @PostMapping("/customer/cart/remove")
    public String removeCart(@RequestParam("foodId") Long id, HttpSession s) {
        getCart(s).removeIf(c -> c.getFoodItem().getId().equals(id));
        return "redirect:/customer/cart";
    }
    @PostMapping("/customer/cart/clear")
    public String clearCart(HttpSession s) { s.removeAttribute("cart"); return "redirect:/customer/cart"; }
    @PostMapping("/customer/order/place")
    public String placeOrder(@RequestParam("pickupDate") @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam("pickupTime") String time,
                             @RequestParam(defaultValue="Counter Payment") String paymentMethod,
                             @RequestParam(defaultValue="PENDING") PaymentStatus paymentStatus,
                             @RequestParam(required=false) String notes,
                             Authentication auth, HttpSession s, RedirectAttributes ra) {
        List<CartItem> cart = getCart(s);
        if (cart.isEmpty()) return "redirect:/customer/menu";
        User user = service.findUserByEmail(auth.getName()).orElseThrow();
        try {
            Order o = service.placePreOrder(user, cart, date, time, paymentMethod, paymentStatus, notes);
            s.removeAttribute("cart");
            ra.addFlashAttribute("successMessage", "Order #" + o.getOrderNumber() + " scheduled for pickup on " + o.getPickupDate() + " at " + o.getPickupTime());
            return "redirect:/customer/orders";
        } catch (Exception e) { ra.addFlashAttribute("errorMessage", e.getMessage()); return "redirect:/customer/cart"; }
    }
    @GetMapping("/customer/orders")
    public String customerOrders(Authentication auth, Model m, HttpSession s) {
        User u = service.findUserByEmail(auth.getName()).orElseThrow();
        m.addAttribute("orders", service.findCustomerOrders(u.getId()));
        m.addAttribute("cartCount", getCartCount(s));
        return "customer/orders";
    }
    @PostMapping("/customer/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User u = service.findUserByEmail(auth.getName()).orElseThrow();
        try { service.cancelCustomerOrder(id, u.getId()); ra.addFlashAttribute("successMessage", "Order cancelled."); }
        catch (Exception e) { ra.addFlashAttribute("errorMessage", e.getMessage()); }
        return "redirect:/customer/orders";
    }

    // --- Vendor Portal ---
    @GetMapping("/vendor/dashboard")
    public String vendorDashboard(@RequestParam(value="filter", required=false) String filter, Model m) {
        List<Order> orders = (filter != null && !filter.equalsIgnoreCase("ALL"))
                ? service.findOrdersByStatus(OrderStatus.valueOf(filter.toUpperCase())) : service.findAllOrders();
        m.addAttribute("orders", orders);
        m.addAttribute("selectedFilter", filter != null ? filter.toUpperCase() : "ALL");
        m.addAttribute("pendingCount", service.getCountByStatus(OrderStatus.PENDING));
        m.addAttribute("preparingCount", service.getCountByStatus(OrderStatus.PREPARING));
        m.addAttribute("readyCount", service.getCountByStatus(OrderStatus.READY));
        m.addAttribute("completedCount", service.getCountByStatus(OrderStatus.COMPLETED));
        return "vendor/dashboard";
    }
    @PostMapping("/vendor/orders/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        service.updateOrderStatus(id, status);
        return "redirect:/vendor/dashboard";
    }
    @PostMapping("/vendor/orders/{id}/payment")
    public String updatePayment(@PathVariable Long id, @RequestParam PaymentStatus paymentStatus) {
        service.updatePaymentStatus(id, paymentStatus);
        return "redirect:/vendor/dashboard";
    }
    @GetMapping("/vendor/menu")
    public String vendorMenu(Model m) { m.addAttribute("foodItems", service.findAllItems()); return "vendor/menu"; }
    @GetMapping("/vendor/menu/new")
    public String newDishForm(Model m) { m.addAttribute("foodItem", new FoodItem()); return "vendor/menu-form"; }
    @GetMapping("/vendor/menu/edit/{id}")
    public String editDishForm(@PathVariable Long id, Model m) {
        service.findItemById(id).ifPresent(i -> m.addAttribute("foodItem", i));
        return "vendor/menu-form";
    }
    @PostMapping("/vendor/menu/save")
    public String saveDish(@Valid @ModelAttribute("foodItem") FoodItem item, BindingResult res) {
        if (res.hasErrors()) return "vendor/menu-form";
        service.saveItem(item);
        return "redirect:/vendor/menu";
    }
    @PostMapping("/vendor/menu/toggle/{id}")
    public String toggleAvailability(@PathVariable Long id) {
        service.toggleItemAvailability(id);
        return "redirect:/vendor/menu";
    }
    @PostMapping("/vendor/menu/delete/{id}")
    public String deleteDish(@PathVariable Long id) {
        service.deleteItem(id);
        return "redirect:/vendor/menu";
    }

    // --- Admin Portal ---
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model m) {
        m.addAttribute("totalUsers", service.getTotalUsers());
        m.addAttribute("totalCustomers", service.findUsersByRole(User.Role.ROLE_CUSTOMER).size());
        m.addAttribute("totalVendors", service.findUsersByRole(User.Role.ROLE_VENDOR).size());
        m.addAttribute("totalMenuItems", service.getTotalItems());
        m.addAttribute("totalOrders", service.getTotalOrders());
        m.addAttribute("completedOrders", service.getCountByStatus(OrderStatus.COMPLETED));
        m.addAttribute("pendingOrders", service.getCountByStatus(OrderStatus.PENDING));
        m.addAttribute("totalRevenue", service.getTotalRevenue());
        m.addAttribute("recentOrders", service.findAllOrders().stream().limit(6).toList());
        m.addAttribute("popularItems", service.getPopularItems(5));
        return "admin/dashboard";
    }
    @GetMapping("/admin/users")
    public String adminUsers(@RequestParam(value="role", required=false) String r, Model m) {
        List<User> users = (r != null && !r.equalsIgnoreCase("ALL"))
                ? service.findUsersByRole(User.Role.valueOf(r.toUpperCase())) : service.findAllUsers();
        m.addAttribute("users", users);
        m.addAttribute("selectedRole", r != null ? r.toUpperCase() : "ALL");
        return "admin/users";
    }
    @PostMapping("/admin/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id) { service.toggleUserActive(id); return "redirect:/admin/users"; }
    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) { service.deleteUser(id); return "redirect:/admin/users"; }

    @GetMapping("/admin/reports")
    public String adminReports(@RequestParam(value="date", required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate d, Model m) {
        LocalDate date = (d != null) ? d : LocalDate.now();
        List<Order> daily = service.findOrdersByDate(date);
        m.addAttribute("reportDate", date);
        m.addAttribute("dailyOrders", daily);
        m.addAttribute("dailyTotalCount", daily.size());
        m.addAttribute("dailyCompleted", daily.stream().filter(o -> o.getOrderStatus() == OrderStatus.COMPLETED).count());
        m.addAttribute("dailyPending", daily.stream().filter(o -> o.getOrderStatus() == OrderStatus.PENDING || o.getOrderStatus() == OrderStatus.CONFIRMED || o.getOrderStatus() == OrderStatus.PREPARING).count());
        m.addAttribute("dailyCancelled", daily.stream().filter(o -> o.getOrderStatus() == OrderStatus.CANCELLED || o.getOrderStatus() == OrderStatus.REJECTED).count());
        m.addAttribute("popularItems", service.getPopularItems(8));
        m.addAttribute("overallTotalOrders", service.getTotalOrders());
        m.addAttribute("overallRevenue", service.getTotalRevenue());
        m.addAttribute("overallCompleted", service.getCountByStatus(OrderStatus.COMPLETED));
        m.addAttribute("overallPending", service.getCountByStatus(OrderStatus.PENDING));
        m.addAttribute("overallCancelled", service.getCountByStatus(OrderStatus.CANCELLED));
        return "admin/reports";
    }
}
