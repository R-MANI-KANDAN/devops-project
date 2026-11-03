package com.licet.foodorder.controller;

import com.licet.foodorder.model.*;
import com.licet.foodorder.service.FoodItemService;
import com.licet.foodorder.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    private final OrderService orderService;
    private final FoodItemService foodItemService;

    public VendorController(OrderService orderService, FoodItemService foodItemService) {
        this.orderService = orderService;
        this.foodItemService = foodItemService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "filter", required = false) String filter, Model model) {
        List<Order> orders;
        if (filter != null && !filter.isBlank() && !filter.equalsIgnoreCase("ALL")) {
            try {
                OrderStatus status = OrderStatus.valueOf(filter.toUpperCase());
                orders = orderService.findOrdersByStatus(status);
            } catch (IllegalArgumentException e) {
                orders = orderService.findAllOrders();
            }
        } else {
            orders = orderService.findAllOrders();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("selectedFilter", filter != null ? filter.toUpperCase() : "ALL");
        model.addAttribute("pendingCount", orderService.getCountByStatus(OrderStatus.PENDING));
        model.addAttribute("preparingCount", orderService.getCountByStatus(OrderStatus.PREPARING));
        model.addAttribute("readyCount", orderService.getCountByStatus(OrderStatus.READY));
        model.addAttribute("completedCount", orderService.getCountByStatus(OrderStatus.COMPLETED));

        return "vendor/dashboard";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") OrderStatus status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " updated to status: " + status);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/orders/{id}/payment")
    public String updatePaymentStatus(@PathVariable("id") Long id,
                                      @RequestParam("paymentStatus") PaymentStatus paymentStatus,
                                      RedirectAttributes redirectAttributes) {
        try {
            orderService.updatePaymentStatus(id, paymentStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " payment marked as: " + paymentStatus);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/vendor/dashboard";
    }

    // Menu Management
    @GetMapping("/menu")
    public String vendorMenu(Model model) {
        model.addAttribute("foodItems", foodItemService.findAll());
        return "vendor/menu";
    }

    @GetMapping("/menu/new")
    public String newFoodItemForm(Model model) {
        if (!model.containsAttribute("foodItem")) {
            model.addAttribute("foodItem", new FoodItem());
        }
        return "vendor/menu-form";
    }

    @GetMapping("/menu/edit/{id}")
    public String editFoodItemForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return foodItemService.findById(id)
                .map(item -> {
                    model.addAttribute("foodItem", item);
                    return "vendor/menu-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Food item not found");
                    return "redirect:/vendor/menu";
                });
    }

    @PostMapping("/menu/save")
    public String saveFoodItem(@Valid @ModelAttribute("foodItem") FoodItem foodItem,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "vendor/menu-form";
        }
        foodItemService.saveOrUpdate(foodItem);
        redirectAttributes.addFlashAttribute("successMessage", "Food item saved successfully!");
        return "redirect:/vendor/menu";
    }

    @PostMapping("/menu/toggle/{id}")
    public String toggleAvailability(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        foodItemService.toggleAvailability(id);
        redirectAttributes.addFlashAttribute("successMessage", "Item availability updated.");
        return "redirect:/vendor/menu";
    }

    @PostMapping("/menu/delete/{id}")
    public String deleteFoodItem(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        foodItemService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Food item removed from menu.");
        return "redirect:/vendor/menu";
    }
}
