package com.example.wavifybe.service;

import com.example.wavifybe.dto.request.FavoriteBatchCheckRequest;
import com.example.wavifybe.dto.response.FavoritePlaylistResponse;
import com.example.wavifybe.dto.response.FavoriteStatusResponse;
import com.example.wavifybe.entity.*;
import com.example.wavifybe.exception.AppException;
import com.example.wavifybe.exception.ErrorCode;
import com.example.wavifybe.mapper.FavoriteMapper;
import com.example.wavifybe.repository.FavoritePlaylistRepository;
import com.example.wavifybe.repository.PlaylistRepository;
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
public class FavoritePlaylistService {
    FavoritePlaylistRepository favoritePlaylistRepository;
    PlaylistRepository playlistRepository;
    UserRepository userRepository;
    FavoriteMapper favoriteMapper;

    @Transactional
    public FavoritePlaylistResponse addFavoritePlaylist(UUID playlistId) {
        User user = getCurrentUser();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAYLIST_NOT_EXISTED));

        if (playlist.getVisibility() == Visibility.PRIVATE
                && !isAdmin()
                && !playlist.getOwner().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (favoritePlaylistRepository.existsByIdUserIdAndIdPlaylistId(user.getId(), playlistId)) {
            throw new AppException(ErrorCode.PLAYLIST_ALREADY_FAVORITED);
        }

        FavoritePlaylist favoritePlaylist = FavoritePlaylist.builder()
                .id(new FavoritePlaylistId(user.getId(), playlistId))
                .user(user)
                .playlist(playlist)
                .build();

        return favoriteMapper.toFavoritePlaylistResponse(favoritePlaylistRepository.save(favoritePlaylist));
    }

    @Transactional
    public void removeFavoritePlaylist(UUID playlistId) {
        User user = getCurrentUser();
        FavoritePlaylist favoritePlaylist = favoritePlaylistRepository
                .findByIdUserIdAndIdPlaylistId(user.getId(), playlistId)
                .orElseThrow(() -> new AppException(ErrorCode.FAVORITE_PLAYLIST_NOT_FOUND));

        favoritePlaylistRepository.delete(favoritePlaylist);
    }

    @Transactional(readOnly = true)
    public Page<FavoritePlaylistResponse> getMyFavoritePlaylists(int page, int size) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return favoritePlaylistRepository
                .findVisibleFavoritesByUserId(user.getId(), isAdmin(), pageable)
                .map(favoriteMapper::toFavoritePlaylistResponse);
    }

    @Transactional(readOnly = true)
    public FavoriteStatusResponse checkFavoritePlaylist(UUID playlistId) {
        User user = getCurrentUser();
        if (!playlistRepository.existsById(playlistId)) {
            throw new AppException(ErrorCode.PLAYLIST_NOT_EXISTED);
        }

        return favoritePlaylistRepository.findByIdUserIdAndIdPlaylistId(user.getId(), playlistId)
                .map(fp -> FavoriteStatusResponse.builder()
                        .favorited(true)
                        .favoritedAt(fp.getCreatedAt())
                        .build())
                .orElseGet(() -> FavoriteStatusResponse.builder()
                        .favorited(false)
                        .favoritedAt(null)
                        .build());
    }

    @Transactional(readOnly = true)
    public Map<UUID, Boolean> checkFavoritePlaylists(FavoriteBatchCheckRequest request) {
        User user = getCurrentUser();
        Set<UUID> uniqueIds = new LinkedHashSet<>(request.getIds());
        Set<UUID> favoritedIds = favoritePlaylistRepository.findFavoritedPlaylistIds(user.getId(), uniqueIds);

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
