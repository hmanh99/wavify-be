package com.example.wavifybe.service;

import com.example.wavifybe.dto.request.PlayHistoryCreationRequest;
import com.example.wavifybe.dto.response.PlayHistoryResponse;
import com.example.wavifybe.entity.PlayHistory;
import com.example.wavifybe.entity.Song;
import com.example.wavifybe.entity.User;
import com.example.wavifybe.entity.Visibility;
import com.example.wavifybe.exception.AppException;
import com.example.wavifybe.exception.ErrorCode;
import com.example.wavifybe.mapper.PlayHistoryMapper;
import com.example.wavifybe.repository.PlayHistoryRepository;
import com.example.wavifybe.repository.SongRepository;
import com.example.wavifybe.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayHistoryService {
    static long DEBOUNCE_SECONDS = 30;

    SongRepository songRepository;
    UserRepository userRepository;
    PlayHistoryMapper playHistoryMapper;
    PlayHistoryRepository playHistoryRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Transactional
    public PlayHistoryResponse recordPlay(PlayHistoryCreationRequest request) {
        User user = getCurrentUser();

        Song song = songRepository.findById(request.getSongId())
                .orElseThrow(() -> new AppException(ErrorCode.SONG_NOT_EXISTED));

        if (song.getVisibility() == Visibility.PRIVATE && !song.getUploader().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Debounce: prevent duplicate inserts if the user played the same song within the debounce window
        var latestPlayOpt = playHistoryRepository.findFirstByUserIdAndSongIdOrderByPlayedAtDesc(user.getId(), song.getId());
        if (latestPlayOpt.isPresent()) {
            PlayHistory latestPlay = latestPlayOpt.get();
            if (latestPlay.getPlayedAt() != null && latestPlay.getPlayedAt().isAfter(Instant.now().minusSeconds(DEBOUNCE_SECONDS))) {
                return playHistoryMapper.toPlayHistoryResponse(latestPlay);
            }
        }

        PlayHistory playHistory = PlayHistory.builder()
                .user(user)
                .song(song)
                .build();

        playHistory = playHistoryRepository.save(playHistory);

        return playHistoryMapper.toPlayHistoryResponse(playHistory);
    }

    @Transactional(readOnly = true)
    public Page<PlayHistoryResponse> getPlayHistoryList(int page, int size) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("playedAt").descending());
        return playHistoryRepository
                .findByUserId(user.getId(), pageable)
                .map(playHistoryMapper::toPlayHistoryResponse);
    }

    @Transactional
    public void clearPlayHistory() {
        User user = getCurrentUser();
        playHistoryRepository.deleteAllByUserId(user.getId());
    }

    @Transactional
    public void deletePlayHistory(UUID historyId) {
        User user = getCurrentUser();
        playHistoryRepository.deleteByIdAndUserId(historyId, user.getId());
    }
}
