package com.example.wavifybe.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    @Email
    String email;

    @Size(min = 8, message = "Password requires 8 character")
    String password;

    @Size(min = 4, message = "Username requires 4 character")
    String username;
}
