package com.example.wavifybe.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddSongToPlaylistRequest {
    @NotEmpty(message = "songIds must not be empty")
    @Size(max = 100, message = "Cannot add more than 100 songs at once")
    List<@NotNull UUID> songIds;
}
