package com.example.wavifybe.dto.request;

import com.example.wavifybe.entity.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlaylistCreationRequest {
    @Size(max=255)
    @NotBlank
    String name;
    Visibility visibility;
}
