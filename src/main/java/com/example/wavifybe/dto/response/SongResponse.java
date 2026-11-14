package com.example.wavifybe.dto.response;

import com.example.wavifybe.entity.Status;
import com.example.wavifybe.entity.Visibility;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SongResponse {
    UUID id;
    String title;
    String artist;
    String album;
    Integer durationSec;
    String fileUrl;
    String coverUrl;
    UUID uploaderId;
    Visibility visibility;
    Status status;
    Instant createdAt;
}
