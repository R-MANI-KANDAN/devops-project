package com.licet.foodorder.controller;

import com.licet.foodorder.model.*;
import com.licet.foodorder.service.FoodItemService;
import com.licet.foodorder.service.OrderService;
import com.licet.foodorder.service.UserService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;
    private final FoodItemService foodItemService;

    public AdminController(UserService userService, OrderService orderService, FoodItemService foodItemService) {
        this.userService = userService;
        this.orderService = orderService;
        this.foodItemService = foodItemService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.getTotalUserCount());
        model.addAttribute("totalCustomers", userService.findByRole(Role.ROLE_CUSTOMER).size());
        model.addAttribute("totalVendors", userService.findByRole(Role.ROLE_VENDOR).size());
        model.addAttribute("totalMenuItems", foodItemService.getTotalCount());
        model.addAttribute("totalOrders", orderService.getTotalOrders());
        model.addAttribute("completedOrders", orderService.getCountByStatus(OrderStatus.COMPLETED));
        model.addAttribute("pendingOrders", orderService.getCountByStatus(OrderStatus.PENDING));
        model.addAttribute("cancelledOrders", orderService.getCountByStatus(OrderStatus.CANCELLED) + orderService.getCountByStatus(OrderStatus.REJECTED));
        model.addAttribute("totalRevenue", orderService.getTotalRevenue());

        model.addAttribute("recentOrders", orderService.findAllOrders().stream().limit(8).toList());
        model.addAttribute("popularItems", orderService.getPopularItems(5));

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(@RequestParam(value = "role", required = false) String roleFilter, Model model) {
        List<User> users;
        if (roleFilter != null && !roleFilter.isBlank() && !roleFilter.equalsIgnoreCase("ALL")) {
            try {
                Role role = Role.valueOf(roleFilter.toUpperCase());
                users = userService.findByRole(role);
            } catch (IllegalArgumentException e) {
                users = userService.findAll();
            }
        } else {
            users = userService.findAll();
        }

        model.addAttribute("users", users);
        model.addAttribute("selectedRole", roleFilter != null ? roleFilter.toUpperCase() : "ALL");
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        userService.toggleActiveStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "User status updated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");
        return "redirect:/admin/users";
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(value = "date", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          Model model) {
        LocalDate reportDate = (date != null) ? date : LocalDate.now();

        List<Order> dailyOrders = orderService.getDailyOrders(reportDate);
        long dailyCompleted = dailyOrders.stream().filter(o -> o.getOrderStatus() == OrderStatus.COMPLETED).count();
        long dailyPending = dailyOrders.stream().filter(o -> o.getOrderStatus() == OrderStatus.PENDING || o.getOrderStatus() == OrderStatus.CONFIRMED || o.getOrderStatus() == OrderStatus.PREPARING).count();
        long dailyCancelled = dailyOrders.stream().filter(o -> o.getOrderStatus() == OrderStatus.CANCELLED || o.getOrderStatus() == OrderStatus.REJECTED).count();

        Map<String, Integer> popularItems = orderService.getPopularItems(10);

        model.addAttribute("reportDate", reportDate);
        model.addAttribute("dailyOrders", dailyOrders);
        model.addAttribute("dailyTotalCount", dailyOrders.size());
        model.addAttribute("dailyCompleted", dailyCompleted);
        model.addAttribute("dailyPending", dailyPending);
        model.addAttribute("dailyCancelled", dailyCancelled);
        model.addAttribute("popularItems", popularItems);

        // Overall stats
        model.addAttribute("overallTotalOrders", orderService.getTotalOrders());
        model.addAttribute("overallRevenue", orderService.getTotalRevenue());
        model.addAttribute("overallCompleted", orderService.getCountByStatus(OrderStatus.COMPLETED));
        model.addAttribute("overallPending", orderService.getCountByStatus(OrderStatus.PENDING));
        model.addAttribute("overallCancelled", orderService.getCountByStatus(OrderStatus.CANCELLED));

        return "admin/reports";
    }
}
