package com.example.wavifybe.service;

import com.example.wavifybe.dto.request.FavoriteBatchCheckRequest;
import com.example.wavifybe.dto.response.FavoriteSongResponse;
import com.example.wavifybe.dto.response.FavoriteStatusResponse;
import com.example.wavifybe.entity.*;
import com.example.wavifybe.exception.AppException;
import com.example.wavifybe.exception.ErrorCode;
import com.example.wavifybe.mapper.FavoriteMapper;
import com.example.wavifybe.repository.FavoriteSongRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FavoriteSongService {
    FavoriteSongRepository favoriteSongRepository;
    SongRepository songRepository;
    UserRepository userRepository;
    FavoriteMapper favoriteMapper;

    @Transactional
    public FavoriteSongResponse addFavoriteSong(UUID songId) {
        User user = getCurrentUser();
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new AppException(ErrorCode.SONG_NOT_EXISTED));

        boolean isPublicReady = song.getVisibility() == Visibility.PUBLIC && song.getStatus() == Status.READY;
        if (!isPublicReady && !isAdmin() && !song.getUploader().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (favoriteSongRepository.existsByIdUserIdAndIdSongId(user.getId(), songId)) {
            throw new AppException(ErrorCode.SONG_ALREADY_FAVORITED);
        }

        FavoriteSong favoriteSong = FavoriteSong.builder()
                .id(new FavoriteSongId(user.getId(), songId))
                .user(user)
                .song(song)
                .build();

        return favoriteMapper.toFavoriteSongResponse(favoriteSongRepository.save(favoriteSong));
    }

    @Transactional
    public void removeFavoriteSong(UUID songId) {
        User user = getCurrentUser();
        FavoriteSong favoriteSong = favoriteSongRepository.findByIdUserIdAndIdSongId(user.getId(), songId)
                .orElseThrow(() -> new AppException(ErrorCode.FAVORITE_SONG_NOT_FOUND));

        favoriteSongRepository.delete(favoriteSong);
    }

    @Transactional(readOnly = true)
    public Page<FavoriteSongResponse> getMyFavoriteSongs(int page, int size) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return favoriteSongRepository
                .findVisibleFavoritesByUserId(user.getId(), isAdmin(), pageable)
                .map(favoriteMapper::toFavoriteSongResponse);
    }

    @Transactional(readOnly = true)
    public FavoriteStatusResponse checkFavoriteSong(UUID songId) {
        User user = getCurrentUser();
        if (!songRepository.existsById(songId)) {
            throw new AppException(ErrorCode.SONG_NOT_EXISTED);
        }

        return favoriteSongRepository.findByIdUserIdAndIdSongId(user.getId(), songId)
                .map(fs -> FavoriteStatusResponse.builder()
                        .favorited(true)
                        .favoritedAt(fs.getCreatedAt())
                        .build())
                .orElseGet(() -> FavoriteStatusResponse.builder()
                        .favorited(false)
                        .favoritedAt(null)
                        .build());
    }

    @Transactional(readOnly = true)
    public Map<UUID, Boolean> checkFavoriteSongs(FavoriteBatchCheckRequest request) {
        User user = getCurrentUser();
        Set<UUID> uniqueIds = new LinkedHashSet<>(request.getIds());
        Set<UUID> favoritedIds = favoriteSongRepository.findFavoritedSongIds(user.getId(), uniqueIds);

        Map<UUID, Boolean> result = new LinkedHashMap<>(uniqueIds.size());
        for (UUID id : uniqueIds) {
            result.put(id, favoritedIds.contains(id));
        }
        return result;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
