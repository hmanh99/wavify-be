package com.example.wavifybe.controller;

import com.example.wavifybe.dto.request.SongCreationRequest;
import com.example.wavifybe.dto.request.SongUpdateRequest;
import com.example.wavifybe.dto.response.ApiResponse;
import com.example.wavifybe.dto.response.SongResponse;
import com.example.wavifybe.service.SongService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/songs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SongController {
    SongService songService;

    @GetMapping
    public ApiResponse<Page<SongResponse>> getSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var response = songService.getSongs(page, size);
        return ApiResponse.<Page<SongResponse>>builder()
                .code(200)
                .result(response)
                .message("Get songs success")
                .build();
    }

    @GetMapping("/my-songs")
    public ApiResponse<List<SongResponse>> getSongsByUploader() {
        var response = songService.getSongsByUploader();
        return ApiResponse.<List<SongResponse>>builder()
                .code(200)
                .result(response)
                .message("Get uploader songs success")
                .build();
    }

    @GetMapping("/{songId}")
    public ApiResponse<SongResponse> getSong(@PathVariable UUID songId) {
        var response = songService.getSong(songId);
        return ApiResponse.<SongResponse>builder()
                .code(200)
                .result(response)
                .message("Get song success")
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SongResponse> uploadSong(
            @RequestPart("data") SongCreationRequest request,
            @RequestPart("audioFile") MultipartFile audioFile,
            @RequestPart("coverFile") MultipartFile coverFile
    ) {
        var response = songService.uploadSong(request, audioFile, coverFile);
        return ApiResponse.<SongResponse>builder()
                .code(200)
                .result(response)
                .message("Upload song success")
                .build();
    }

    @PutMapping("/{songId}")
    public ApiResponse<SongResponse> updateSong(
            @PathVariable UUID songId,
            @RequestBody SongUpdateRequest request
    ) {
        var response = songService.updateSong(songId, request);
        return ApiResponse.<SongResponse>builder()
                .code(200)
                .result(response)
                .message("Update song success")
                .build();
    }

    @DeleteMapping("/{songId}")
    public ApiResponse<Void> deleteSong(@PathVariable UUID songId) {
        songService.deleteSong(songId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Delete song success")
                .build();
    }
}
