package com.licet.foodorder.config;

import com.licet.foodorder.model.*;
import com.licet.foodorder.repository.FoodItemRepository;
import com.licet.foodorder.repository.OrderRepository;
import com.licet.foodorder.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository,
                      FoodItemRepository foodItemRepository,
                      OrderRepository orderRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.foodItemRepository = foodItemRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed Users
        User admin = new User(null, "System Administrator", "admin@foodhub.com",
                passwordEncoder.encode("admin123"), "9876543210", Role.ROLE_ADMIN, true);
        userRepository.save(admin);

        User vendor = new User(null, "Campus Cafe Chef", "vendor@foodhub.com",
                passwordEncoder.encode("vendor123"), "9876543211", Role.ROLE_VENDOR, true);
        userRepository.save(vendor);

        User customer1 = new User(null, "Aksheen Jane", "customer@foodhub.com",
                passwordEncoder.encode("customer123"), "9876543212", Role.ROLE_CUSTOMER, true);
        userRepository.save(customer1);

        User customer2 = new User(null, "Mani Kandan", "mani@foodhub.com",
                passwordEncoder.encode("customer123"), "9876543213", Role.ROLE_CUSTOMER, true);
        userRepository.save(customer2);

        // Seed Menu Items
        FoodItem item1 = new FoodItem(null, "Hyderabadi Chicken Biryani", "Biryani",
                new BigDecimal("220.00"), "Aromatic long-grain basmati rice cooked with succulent chicken and rich spices.", true, "/images/biryani.jpg");
        FoodItem item2 = new FoodItem(null, "Paneer Tikka Roll", "Starters",
                new BigDecimal("130.00"), "Spiced grilled paneer cubes wrapped in layered paratha with mint chutney.", true, "/images/paneer-roll.jpg");
        FoodItem item3 = new FoodItem(null, "Crispy Chicken Burger", "Burger",
                new BigDecimal("150.00"), "Juicy crispy patty topped with iceberg lettuce, pickles, and spicy garlic mayo.", true, "/images/burger.jpg");
        FoodItem item4 = new FoodItem(null, "Margherita Farmhouse Pizza", "Pizza",
                new BigDecimal("240.00"), "Fresh mozzarella, bell peppers, sweet corn, and aromatic basil on crust.", true, "/images/pizza.jpg");
        FoodItem item5 = new FoodItem(null, "Peri Peri French Fries", "Starters",
                new BigDecimal("90.00"), "Golden crispy French fries seasoned generously with zesty peri-peri spice mix.", true, "/images/fries.jpg");
        FoodItem item6 = new FoodItem(null, "Cold Coffee with Vanilla", "Beverages",
                new BigDecimal("80.00"), "Chilled freshly brewed coffee blended thick and topped with creamy vanilla.", true, "/images/coffee.jpg");
        FoodItem item7 = new FoodItem(null, "Molten Chocolate Lava Cake", "Dessert",
                new BigDecimal("120.00"), "Decadent warm chocolate cake filled with rich flowing dark chocolate lava.", true, "/images/lavacake.jpg");
        FoodItem item8 = new FoodItem(null, "Fresh Mint Lemonade", "Beverages",
                new BigDecimal("50.00"), "Invigorating blend of freshly squeezed lemon, crushed mint leaves, and ice.", true, "/images/lemonade.jpg");

        foodItemRepository.save(item1);
        foodItemRepository.save(item2);
        foodItemRepository.save(item3);
        foodItemRepository.save(item4);
        foodItemRepository.save(item5);
        foodItemRepository.save(item6);
        foodItemRepository.save(item7);
        foodItemRepository.save(item8);

        // Seed Sample Orders
        OrderItem oi1 = new OrderItem(1L, item1.getId(), item1.getName(), item1.getPrice(), 1);
        OrderItem oi2 = new OrderItem(2L, item6.getId(), item6.getName(), item6.getPrice(), 1);
        List<OrderItem> list1 = Arrays.asList(oi1, oi2);
        BigDecimal total1 = oi1.getSubtotal().add(oi2.getSubtotal());

        Order order1 = new Order(null, "ORD-10001", customer1.getId(), customer1.getName(),
                customer1.getEmail(), customer1.getPhone(), LocalDate.now(), "13:30", list1, total1,
                PaymentStatus.PAID, "UPI / Online", OrderStatus.COMPLETED, "Please keep it warm.");
        orderRepository.save(order1);

        OrderItem oi3 = new OrderItem(3L, item3.getId(), item3.getName(), item3.getPrice(), 2);
        OrderItem oi4 = new OrderItem(4L, item5.getId(), item5.getName(), item5.getPrice(), 1);
        List<OrderItem> list2 = Arrays.asList(oi3, oi4);
        BigDecimal total2 = oi3.getSubtotal().add(oi4.getSubtotal());

        Order order2 = new Order(null, "ORD-10002", customer2.getId(), customer2.getName(),
                customer2.getEmail(), customer2.getPhone(), LocalDate.now().plusDays(1), "12:15", list2, total2,
                PaymentStatus.PENDING, "Counter Payment", OrderStatus.PREPARING, "Extra napkins please.");
        orderRepository.save(order2);

        OrderItem oi5 = new OrderItem(5L, item4.getId(), item4.getName(), item4.getPrice(), 1);
        List<OrderItem> list3 = Arrays.asList(oi5);
        BigDecimal total3 = oi5.getSubtotal();

        Order order3 = new Order(null, "ORD-10003", customer1.getId(), customer1.getName(),
                customer1.getEmail(), customer1.getPhone(), LocalDate.now().plusDays(1), "18:00", list3, total3,
                PaymentStatus.PENDING, "Counter Payment", OrderStatus.PENDING, "Pickup at evening break.");
        orderRepository.save(order3);
    }
}
