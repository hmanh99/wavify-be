package com.example.wavifybe.dto.request;


import com.example.wavifybe.entity.Visibility;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SongUpdateRequest {
    String title;
    String artist;
    String album;
    Visibility visibility;
}
