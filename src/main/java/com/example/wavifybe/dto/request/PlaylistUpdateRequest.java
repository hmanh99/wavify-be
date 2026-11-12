package com.example.wavifybe.dto.request;

import com.example.wavifybe.entity.Visibility;
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
public class PlaylistUpdateRequest {
    @Size(min = 1, max = 255)
    String name;
    Visibility visibility;
}
