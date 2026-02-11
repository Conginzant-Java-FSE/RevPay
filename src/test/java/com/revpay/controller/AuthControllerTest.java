package com.revpay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revpay.dto.ForgotPasswordRequest;
import com.revpay.dto.ForgotPasswordResponse;
import com.revpay.exception.GlobalExceptionHandler;
import com.revpay.exception.SecurityAnswerMismatchException;
import com.revpay.exception.UserNotFoundException;
import com.revpay.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /api/auth/forgot-password - success returns 200")
    void forgotPassword_success_returns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(
                "john@example.com", "What is your pet's name?", "Buddy", "NewSecure@123");

        ForgotPasswordResponse response = new ForgotPasswordResponse("Password reset successfully", true);
        when(userService.forgotPassword(any(ForgotPasswordRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - user not found returns 404")
    void forgotPassword_userNotFound_returns404() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(
                "unknown@example.com", "What is your pet's name?", "Buddy", "NewSecure@123");

        when(userService.forgotPassword(any(ForgotPasswordRequest.class)))
                .thenThrow(new UserNotFoundException("No user found with the provided email or phone number"));

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No user found with the provided email or phone number"));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - wrong answer returns 401")
    void forgotPassword_wrongAnswer_returns401() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(
                "john@example.com", "What is your pet's name?", "WrongAnswer", "NewSecure@123");

        when(userService.forgotPassword(any(ForgotPasswordRequest.class)))
                .thenThrow(new SecurityAnswerMismatchException("Security answer is incorrect"));

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Security answer is incorrect"));
    }
}
