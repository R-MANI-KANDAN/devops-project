package com.licet.foodorder.config;

import com.licet.foodorder.model.*;
import com.licet.foodorder.model.Order.*;
import com.licet.foodorder.repository.AppRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppRepository repo) {
        return email -> repo.findUserByEmail(email)
                .filter(User::isActive)
                .map(u -> new org.springframework.security.core.userdetails.User(
                        u.getEmail(), u.getPassword(),
                        Collections.singletonList(new SimpleGrantedAuthority(u.getRole().name()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found or inactive: " + email));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/customer/**").hasAuthority("ROLE_CUSTOMER")
                .requestMatchers("/vendor/**").hasAuthority("ROLE_VENDOR")
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login").defaultSuccessUrl("/home", true).failureUrl("/login?error=true").permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout").logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true).deleteCookies("JSESSIONID").permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/login?denied=true"));
        return http.build();
    }

    @Bean
    public CommandLineRunner dataLoader(AppRepository repo, PasswordEncoder enc) {
        return args -> {
            User admin = repo.saveUser(new User(null, "System Administrator", "admin@foodhub.com", enc.encode("admin123"), "9876543210", User.Role.ROLE_ADMIN, true));
            User vendor = repo.saveUser(new User(null, "Campus Cafe Chef", "vendor@foodhub.com", enc.encode("vendor123"), "9876543211", User.Role.ROLE_VENDOR, true));
            User cust1 = repo.saveUser(new User(null, "Aksheen Jane", "customer@foodhub.com", enc.encode("customer123"), "9876543212", User.Role.ROLE_CUSTOMER, true));
            User cust2 = repo.saveUser(new User(null, "Mani Kandan", "mani@foodhub.com", enc.encode("customer123"), "9876543213", User.Role.ROLE_CUSTOMER, true));

            FoodItem f1 = repo.saveItem(new FoodItem(null, "Hyderabadi Chicken Biryani", "Biryani", new BigDecimal("220.00"), "Aromatic basmati rice cooked with succulent chicken and rich spices.", true));
            FoodItem f2 = repo.saveItem(new FoodItem(null, "Paneer Tikka Roll", "Starters", new BigDecimal("130.00"), "Spiced grilled paneer cubes wrapped in layered paratha with mint chutney.", true));
            FoodItem f3 = repo.saveItem(new FoodItem(null, "Crispy Chicken Burger", "Burger", new BigDecimal("150.00"), "Juicy crispy patty topped with iceberg lettuce and spicy mayo.", true));
            FoodItem f4 = repo.saveItem(new FoodItem(null, "Margherita Farmhouse Pizza", "Pizza", new BigDecimal("240.00"), "Fresh mozzarella, bell peppers, and basil on crispy crust.", true));
            FoodItem f5 = repo.saveItem(new FoodItem(null, "Peri Peri French Fries", "Starters", new BigDecimal("90.00"), "Crispy French fries dusted with zesty peri-peri seasoning.", true));
            FoodItem f6 = repo.saveItem(new FoodItem(null, "Cold Coffee with Vanilla", "Beverages", new BigDecimal("80.00"), "Chilled freshly brewed coffee topped with rich vanilla ice cream.", true));
            FoodItem f7 = repo.saveItem(new FoodItem(null, "Molten Chocolate Lava Cake", "Dessert", new BigDecimal("120.00"), "Warm chocolate cake filled with flowing dark chocolate lava.", true));
            FoodItem f8 = repo.saveItem(new FoodItem(null, "Fresh Mint Lemonade", "Beverages", new BigDecimal("50.00"), "Refreshing blend of freshly squeezed lemon, crushed mint and soda.", true));

            // Seed Sample Pre-Orders
            Order o1 = new Order();
            o1.setCustomerId(cust1.getId()); o1.setCustomerName(cust1.getName()); o1.setCustomerEmail(cust1.getEmail()); o1.setCustomerPhone(cust1.getPhone());
            o1.setPickupDate(LocalDate.now()); o1.setPickupTime("12:30 PM");
            o1.setItems(List.of(new OrderItem(1L, f1.getId(), f1.getName(), f1.getPrice(), 1), new OrderItem(2L, f6.getId(), f6.getName(), f6.getPrice(), 1)));
            o1.setTotalAmount(new BigDecimal("300.00")); o1.setPaymentStatus(PaymentStatus.PAID); o1.setOrderStatus(OrderStatus.COMPLETED);
            repo.saveOrder(o1);

            Order o2 = new Order();
            o2.setCustomerId(cust2.getId()); o2.setCustomerName(cust2.getName()); o2.setCustomerEmail(cust2.getEmail()); o2.setCustomerPhone(cust2.getPhone());
            o2.setPickupDate(LocalDate.now().plusDays(1)); o2.setPickupTime("13:15");
            o2.setItems(List.of(new OrderItem(3L, f3.getId(), f3.getName(), f3.getPrice(), 2)));
            o2.setTotalAmount(new BigDecimal("300.00")); o2.setPaymentStatus(PaymentStatus.PENDING); o2.setOrderStatus(OrderStatus.PREPARING);
            repo.saveOrder(o2);
        };
    }
}
