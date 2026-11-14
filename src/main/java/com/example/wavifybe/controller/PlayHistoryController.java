package com.example.wavifybe.controller;

import com.example.wavifybe.dto.request.PlayHistoryCreationRequest;
import com.example.wavifybe.dto.response.ApiResponse;
import com.example.wavifybe.dto.response.PlayHistoryResponse;
import com.example.wavifybe.service.PlayHistoryService;
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

import java.util.UUID;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PlayHistoryController {
    PlayHistoryService playHistoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlayHistoryResponse> recordPlay(@RequestBody @Valid PlayHistoryCreationRequest request) {
        var result = playHistoryService.recordPlay(request);
        return ApiResponse.<PlayHistoryResponse>builder()
                .code(201)
                .result(result)
                .message("Record play history successful")
                .build();
    }

    @GetMapping
    public ApiResponse<Page<PlayHistoryResponse>> getPlayHistoryList(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        var result = playHistoryService.getPlayHistoryList(page, size);
        return ApiResponse.<Page<PlayHistoryResponse>>builder()
                .code(200)
                .result(result)
                .message("Get play history successful")
                .build();
    }

    @DeleteMapping
    public ApiResponse<Void> clearPlayHistory() {
        playHistoryService.clearPlayHistory();
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Clear play history successful")
                .build();
    }

    @DeleteMapping("/{historyId}")
    public ApiResponse<Void> deletePlayHistoryItem(@PathVariable UUID historyId) {
        playHistoryService.deletePlayHistory(historyId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Delete play history item successful")
                .build();
    }
}
