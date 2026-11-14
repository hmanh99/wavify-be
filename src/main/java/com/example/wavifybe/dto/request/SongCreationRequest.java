package com.example.wavifybe.dto.request;

import com.example.wavifybe.entity.Visibility;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SongCreationRequest {
    String title;
    String artist;
    String album;
    Visibility visibility;
}
