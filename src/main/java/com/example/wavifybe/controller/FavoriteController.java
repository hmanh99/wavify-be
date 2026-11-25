package com.example.wavifybe.controller;

import com.example.wavifybe.dto.request.FavoriteBatchCheckRequest;
import com.example.wavifybe.dto.response.ApiResponse;
import com.example.wavifybe.dto.response.FavoritePlaylistResponse;
import com.example.wavifybe.dto.response.FavoriteSongResponse;
import com.example.wavifybe.dto.response.FavoriteStatusResponse;
import com.example.wavifybe.service.FavoritePlaylistService;
import com.example.wavifybe.service.FavoriteSongService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FavoriteController {
    FavoriteSongService favoriteSongService;
    FavoritePlaylistService favoritePlaylistService;

    @GetMapping("/songs")
    public ApiResponse<Page<FavoriteSongResponse>> getMyFavoriteSongs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        var result = favoriteSongService.getMyFavoriteSongs(page, size);
        return ApiResponse.<Page<FavoriteSongResponse>>builder()
                .code(200)
                .result(result)
                .message("Get favorite songs successful")
                .build();
    }

    @PostMapping("/songs/{songId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteSongResponse> addFavoriteSong(@PathVariable UUID songId) {
        var result = favoriteSongService.addFavoriteSong(songId);
        return ApiResponse.<FavoriteSongResponse>builder()
                .code(201)
                .result(result)
                .message("Add song to favorites successful")
                .build();
    }

    @DeleteMapping("/songs/{songId}")
    public ApiResponse<Void> removeFavoriteSong(@PathVariable UUID songId) {
        favoriteSongService.removeFavoriteSong(songId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Remove song from favorites successful")
                .build();
    }

    @GetMapping("/songs/{songId}/check")
    public ApiResponse<FavoriteStatusResponse> checkFavoriteSong(@PathVariable UUID songId) {
        var result = favoriteSongService.checkFavoriteSong(songId);
        return ApiResponse.<FavoriteStatusResponse>builder()
                .code(200)
                .result(result)
                .message("Check favorite song status successful")
                .build();
    }

    @PostMapping("/songs/check")
    public ApiResponse<Map<UUID, Boolean>> checkFavoriteSongs(
            @RequestBody @Valid FavoriteBatchCheckRequest request) {
        var result = favoriteSongService.checkFavoriteSongs(request);
        return ApiResponse.<Map<UUID, Boolean>>builder()
                .code(200)
                .result(result)
                .message("Check favorite songs status successful")
                .build();
    }

    @GetMapping("/playlists")
    public ApiResponse<Page<FavoritePlaylistResponse>> getMyFavoritePlaylists(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        var result = favoritePlaylistService.getMyFavoritePlaylists(page, size);
        return ApiResponse.<Page<FavoritePlaylistResponse>>builder()
                .code(200)
                .result(result)
                .message("Get favorite playlists successful")
                .build();
    }

    @PostMapping("/playlists/{playlistId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoritePlaylistResponse> addFavoritePlaylist(@PathVariable UUID playlistId) {
        var result = favoritePlaylistService.addFavoritePlaylist(playlistId);
        return ApiResponse.<FavoritePlaylistResponse>builder()
                .code(201)
                .result(result)
                .message("Add playlist to favorites successful")
                .build();
    }

    @DeleteMapping("/playlists/{playlistId}")
    public ApiResponse<Void> removeFavoritePlaylist(@PathVariable UUID playlistId) {
        favoritePlaylistService.removeFavoritePlaylist(playlistId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Remove playlist from favorites successful")
                .build();
    }

    @GetMapping("/playlists/{playlistId}/check")
    public ApiResponse<FavoriteStatusResponse> checkFavoritePlaylist(@PathVariable UUID playlistId) {
        var result = favoritePlaylistService.checkFavoritePlaylist(playlistId);
        return ApiResponse.<FavoriteStatusResponse>builder()
                .code(200)
                .result(result)
                .message("Check favorite playlist status successful")
                .build();
    }

    @PostMapping("/playlists/check")
    public ApiResponse<Map<UUID, Boolean>> checkFavoritePlaylists(
            @RequestBody @Valid FavoriteBatchCheckRequest request) {
        var result = favoritePlaylistService.checkFavoritePlaylists(request);
        return ApiResponse.<Map<UUID, Boolean>>builder()
                .code(200)
                .result(result)
                .message("Check favorite playlists status successful")
                .build();
    }
}
