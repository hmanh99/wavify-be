package com.example.wavifybe.controller;

import com.example.wavifybe.dto.request.AddSongToPlaylistRequest;
import com.example.wavifybe.dto.request.PlaylistCreationRequest;
import com.example.wavifybe.dto.request.PlaylistUpdateRequest;
import com.example.wavifybe.dto.request.ReorderPlaylistSongsRequest;
import com.example.wavifybe.dto.response.ApiResponse;
import com.example.wavifybe.dto.response.PlaylistResponse;
import com.example.wavifybe.dto.response.PlaylistSongResponse;
import com.example.wavifybe.entity.Playlist;
import com.example.wavifybe.service.PlaylistService;
import com.example.wavifybe.service.PlaylistSongService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/playlists")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PlaylistController {
    PlaylistService playlistService;
    PlaylistSongService playlistSongService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlaylistResponse> createPlaylist(@RequestBody @Valid PlaylistCreationRequest request) {
        var result = playlistService.createPlaylist(request);
        return ApiResponse.<PlaylistResponse>builder()
                .code(201)
                .result(result)
                .message("Create playlist successful")
                .build();
    }

    @GetMapping("/my-playlists")
    public ApiResponse<List<PlaylistResponse>> getMyPlaylists() {
        var result = playlistService.getMyPlaylists();
        return ApiResponse.<List<PlaylistResponse>>builder()
                .code(200)
                .result(result)
                .message("Get current user playlists successful")
                .build();
    }

    @GetMapping("/{playlistId}")
    public ApiResponse<PlaylistResponse> getPlaylist(@PathVariable UUID playlistId) {
        var result = playlistService.getPlaylist(playlistId);
        return ApiResponse.<PlaylistResponse>builder()
                .code(200)
                .result(result)
                .message("Get playlist successful")
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<Page<PlaylistResponse>> searchPublicPlaylists(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50)int size) {
        var result = playlistService.searchPublicPlaylists(keyword, page, size);
        return ApiResponse.<Page<PlaylistResponse>>builder()
                .code(200)
                .result(result)
                .message("Search playlists successful")
                .build();
    }

    @GetMapping("/owner/{ownerId}")
    public ApiResponse<List<PlaylistResponse>> getPublicPlaylistsByOwner(@PathVariable UUID ownerId) {
        var result = playlistService.getPublicPlaylistsByOwner(ownerId);
        return ApiResponse.<List<PlaylistResponse>>builder()
                .code(200)
                .result(result)
                .message("Search playlists by owner successful")
                .build();
    }

    @PutMapping("/{playlistId}")
    public ApiResponse<PlaylistResponse> updatePlaylist(
            @PathVariable UUID playlistId,
            @RequestBody @Valid PlaylistUpdateRequest request) {
        var result = playlistService.updatePlaylist(playlistId, request);
        return ApiResponse.<PlaylistResponse>builder()
                .code(200)
                .result(result)
                .message("Update playlist successful")
                .build();
    }

    @DeleteMapping("/{playlistId}")
    public ApiResponse<Void> deletePlaylist(
            @PathVariable UUID playlistId) {
        playlistService.deletePlaylist(playlistId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Delete playlist successful")
                .build();
    }

    @GetMapping("/{playlistId}/songs")
    public ApiResponse<List<PlaylistSongResponse>> getPlaylistSongs(@PathVariable UUID playlistId){
        var result = playlistSongService.getPlaylistSongs(playlistId);
        return ApiResponse.<List<PlaylistSongResponse>>builder()
                .code(200)
                .result(result)
                .message("Get songs in playlist successful")
                .build();
    }

    @PostMapping("/{playlistId}/songs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<PlaylistSongResponse>> addSongsToPlaylist(
            @PathVariable UUID playlistId,
            @RequestBody @Valid AddSongToPlaylistRequest request){
        var result = playlistSongService.addSongsToPlaylist(playlistId,request);
        return ApiResponse.<List<PlaylistSongResponse>>builder()
                .code(201)
                .result(result)
                .message("Add song to playlist successful")
                .build();
    }
    @DeleteMapping("/{playlistId}/songs/{songId}")
    public ApiResponse<Void> removeSongFromPlaylist(
            @PathVariable UUID playlistId,
            @PathVariable UUID songId ){
        playlistSongService.removeSongFromPlaylist(playlistId,songId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Delete song to playlist successful")
                .build();
    }

    @PutMapping("/{playlistId}/songs/reorder")
    public ApiResponse<List<PlaylistSongResponse>> reorderPlaylistSongs(
            @PathVariable UUID playlistId,
            @RequestBody @Valid ReorderPlaylistSongsRequest request) {
        var result = playlistSongService.reorderPlaylistSongs(playlistId, request);
        return ApiResponse.<List<PlaylistSongResponse>>builder()
                .code(200)
                .result(result)
                .message("Reorder playlist songs successful")
                .build();
    }
}
