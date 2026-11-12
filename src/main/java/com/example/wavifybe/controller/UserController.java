package com.example.wavifybe.controller;

import com.example.wavifybe.dto.request.UserUpdateRequest;
import com.example.wavifybe.dto.response.ApiResponse;
import com.example.wavifybe.dto.response.UserResponse;
import com.example.wavifybe.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;

    @GetMapping("/current-user")
    public ApiResponse<UserResponse> getCurrentUser() {
        var response = userService.getCurrentUser();
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .result(response)
                .message("Get current user info successful")
                .build();
    }
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID userId) {
        var response = userService.getUser(userId);
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .result(response)
                .message("Get user successful")
                .build();
    }
    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {
        var response = userService.getUsers();
        return ApiResponse.<List<UserResponse>>builder()
                .code(200)
                .result(response)
                .message("Get users successful")
                .build();
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(@PathVariable UUID userId,@RequestBody UserUpdateRequest request){
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .result(userService.updateUser(userId, request))
                .message("Update user successful")
                .build();
    }
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> updateUser(@PathVariable UUID userId){
        userService.deleteUser(userId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Delete user successful")
                .build();
    }
}
