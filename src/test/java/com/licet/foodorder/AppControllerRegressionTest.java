package com.licet.foodorder;

import com.licet.foodorder.controller.AppController;
import com.licet.foodorder.model.User;
import com.licet.foodorder.service.AppService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AppControllerRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppService service;

    @Test
    @DisplayName("Test 1: GET /login should display login page successfully")
    void testLoginPageDisplay() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("Test 2: POST /register with valid details should redirect to /login")
    void testRegisterWithValidDetails() throws Exception {
        User registeredUser = new User(10L, "New Customer", "newuser@foodhub.com", "pass123", "9876543210", User.Role.ROLE_CUSTOMER, true);
        when(service.registerUser(any(User.class))).thenReturn(registeredUser);

        mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "New Customer")
                .param("email", "newuser@foodhub.com")
                .param("password", "pass123")
                .param("phone", "9876543210"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage", "Registration successful! Please login."));
    }

    @Test
    @DisplayName("Test 3: POST /register with duplicate email should show error")
    void testRegisterWithDuplicateEmail() throws Exception {
        when(service.registerUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Email already registered."));

        mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "Existing User")
                .param("email", "admin@foodhub.com")
                .param("password", "pass123")
                .param("phone", "9876543210"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("Test 4: Customer accessing /customer/menu with ROLE_CUSTOMER should succeed")
    @WithMockUser(username = "customer@foodhub.com", authorities = {"ROLE_CUSTOMER"})
    void testCustomerMenuAccessAllowed() throws Exception {
        mockMvc.perform(get("/customer/menu"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer/menu"));
    }

    @Test
    @DisplayName("Test 5: Customer attempting to access /admin/dashboard should be denied (403)")
    @WithMockUser(username = "customer@foodhub.com", authorities = {"ROLE_CUSTOMER"})
    void testCustomerAccessAdminDenied() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?denied=true"));
    }
}
