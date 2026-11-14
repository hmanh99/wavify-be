package com.example.wavifybe.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayHistoryResponse {
    UUID id;
    UUID userId;
    SongResponse song;
    Instant playedAt;
}

