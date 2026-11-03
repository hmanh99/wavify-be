package com.example.wavifybe.dto.response;

import com.example.wavifybe.entity.UserRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    UUID id;
    String email;
    String username;
    UserRole role;
    Instant created_at;
}