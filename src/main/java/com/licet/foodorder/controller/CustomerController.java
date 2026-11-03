package com.licet.foodorder.controller;

import com.licet.foodorder.model.*;
import com.licet.foodorder.service.FoodItemService;
import com.licet.foodorder.service.OrderService;
import com.licet.foodorder.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final FoodItemService foodItemService;
    private final OrderService orderService;
    private final UserService userService;

    public CustomerController(FoodItemService foodItemService, OrderService orderService, UserService userService) {
        this.foodItemService = foodItemService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @SuppressWarnings("unchecked")
    private List<CartItem> getCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    private int getCartCount(HttpSession session) {
        return getCart(session).stream().mapToInt(CartItem::getQuantity).sum();
    }

    @GetMapping("/menu")
    public String showMenu(@RequestParam(value = "search", required = false) String search,
                           @RequestParam(value = "category", required = false, defaultValue = "ALL") String category,
                           Model model,
                           HttpSession session) {
        List<FoodItem> items = foodItemService.searchItems(search, category, true);
        List<String> categories = foodItemService.getCategories();

        model.addAttribute("foodItems", items);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("cartCount", getCartCount(session));

        return "customer/menu";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("foodId") Long foodId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Optional<FoodItem> foodOpt = foodItemService.findById(foodId);
        if (foodOpt.isPresent()) {
            FoodItem foodItem = foodOpt.get();
            if (!foodItem.isAvailable()) {
                redirectAttributes.addFlashAttribute("errorMessage", foodItem.getName() + " is currently unavailable.");
                return "redirect:/customer/menu";
            }

            List<CartItem> cart = getCart(session);
            boolean exists = false;
            for (CartItem ci : cart) {
                if (ci.getFoodItem().getId().equals(foodId)) {
                    ci.setQuantity(ci.getQuantity() + quantity);
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                cart.add(new CartItem(foodItem, quantity));
            }
            redirectAttributes.addFlashAttribute("successMessage", "Added " + foodItem.getName() + " to cart!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected food item does not exist.");
        }
        return "redirect:/customer/menu";
    }

    @GetMapping("/cart")
    public String viewCart(Model model, HttpSession session) {
        List<CartItem> cart = getCart(session);
        BigDecimal total = cart.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cartItems", cart);
        model.addAttribute("totalAmount", total);
        model.addAttribute("cartCount", getCartCount(session));
        model.addAttribute("todayDate", LocalDate.now().toString());
        model.addAttribute("maxDate", LocalDate.now().plusDays(7).toString());

        return "customer/cart";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(@RequestParam("foodId") Long foodId,
                                 @RequestParam("quantity") int quantity,
                                 HttpSession session) {
        List<CartItem> cart = getCart(session);
        if (quantity <= 0) {
            cart.removeIf(ci -> ci.getFoodItem().getId().equals(foodId));
        } else {
            for (CartItem ci : cart) {
                if (ci.getFoodItem().getId().equals(foodId)) {
                    ci.setQuantity(quantity);
                    break;
                }
            }
        }
        return "redirect:/customer/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam("foodId") Long foodId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        List<CartItem> cart = getCart(session);
        cart.removeIf(ci -> ci.getFoodItem().getId().equals(foodId));
        redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart.");
        return "redirect:/customer/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        session.removeAttribute("cart");
        return "redirect:/customer/cart";
    }

    @PostMapping("/order/place")
    public String placePreOrder(@RequestParam("pickupDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDate,
                                @RequestParam("pickupTime") String pickupTime,
                                @RequestParam(value = "paymentMethod", defaultValue = "Counter Payment") String paymentMethod,
                                @RequestParam(value = "paymentStatus", defaultValue = "PENDING") PaymentStatus paymentStatus,
                                @RequestParam(value = "notes", required = false) String notes,
                                Authentication authentication,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        List<CartItem> cart = getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty. Add items before ordering.");
            return "redirect:/customer/menu";
        }

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        try {
            Order order = orderService.createPreOrder(user, cart, pickupDate, pickupTime, paymentMethod, paymentStatus, notes);
            session.removeAttribute("cart");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Pre-Order placed successfully! Your Order ID is #" + order.getOrderNumber() +
                            ". Scheduled for pickup on " + order.getPickupDate() + " at " + order.getPickupTime());
            return "redirect:/customer/orders";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer/cart";
        }
    }

    @GetMapping("/orders")
    public String customerOrders(Authentication authentication, Model model, HttpSession session) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        List<Order> orders = orderService.findOrdersByCustomer(user.getId());
        model.addAttribute("orders", orders);
        model.addAttribute("cartCount", getCartCount(session));
        model.addAttribute("userName", user.getName());
        return "customer/orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable("id") Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(authentication.getName()).orElseThrow();
        try {
            orderService.cancelOrderByCustomer(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " has been cancelled.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/orders";
    }
}
