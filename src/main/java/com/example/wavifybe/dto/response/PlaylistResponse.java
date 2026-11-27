package com.example.wavifybe.dto.response;

import com.example.wavifybe.entity.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlaylistResponse {
    UUID id;
    UserResponse owner;
    String name;
    Visibility visibility;
    Instant createdAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<SongResponse> songs;
}
