package com.example.wavifybe.controller;

import com.example.wavifybe.dto.request.AuthRequest;
import com.example.wavifybe.dto.request.RefreshRequest;
import com.example.wavifybe.dto.request.RegisterRequest;
import com.example.wavifybe.dto.response.ApiResponse;
import com.example.wavifybe.dto.response.AuthResponse;
import com.example.wavifybe.dto.response.UserResponse;
import com.example.wavifybe.service.AuthService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthController {
    AuthService authService;

    @PostMapping("/register")
    ApiResponse<UserResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(201)
                .result(authService.register(request))
                .message("Create user successful")
                .build();
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .code(200)
                .result(authService.login(request))
                .message("Login successful")
                .build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .code(200)
                .result(authService.refresh(request))
                .message("Refresh token successful")
                .build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody @Valid RefreshRequest request) {
        authService.logout(request);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Logout successful")
                .build();
    }
}
