package com.example.wavifybe.service;

import com.example.wavifybe.dto.request.PlaylistCreationRequest;
import com.example.wavifybe.dto.request.PlaylistUpdateRequest;
import com.example.wavifybe.dto.response.PlaylistResponse;
import com.example.wavifybe.dto.response.SongResponse;
import com.example.wavifybe.entity.Playlist;
import com.example.wavifybe.entity.Song;
import com.example.wavifybe.entity.User;
import com.example.wavifybe.entity.Visibility;
import com.example.wavifybe.exception.AppException;
import com.example.wavifybe.exception.ErrorCode;
import com.example.wavifybe.mapper.PlaylistMapper;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlaylistService {
    PlaylistRepository playlistRepository;
    UserRepository userRepository;
    FavoritePlaylistRepository favoritePlaylistRepository;
    PlaylistMapper playlistMapper;

    @Transactional
    public PlaylistResponse createPlaylist(PlaylistCreationRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Playlist playlist = playlistMapper.toPlaylist(request);
        playlist.setOwner(owner);
        if (playlist.getVisibility() == null) {
            playlist.setVisibility(Visibility.PRIVATE);
        }

        return playlistMapper.toPlaylistResponse(playlistRepository.save(playlist));
    }

    @Transactional(readOnly = true)
    public PlaylistResponse getPlaylist(UUID playlistId) {
        Playlist playlist = playlistRepository.findDetailById(playlistId).orElseThrow(() -> new AppException(ErrorCode.PLAYLIST_NOT_EXISTED));

        if (playlist.getVisibility() == Visibility.PRIVATE) {
            validatePermission(playlist);
        }

        PlaylistResponse response = playlistMapper.toPlaylistResponse(playlist);
        List<SongResponse> visibleSongs = playlist.getSongs().stream()
                .filter(ps -> canViewSong(ps.getSong()))
                .map(playlistMapper::toSongResponse)
                .toList();
        response.setSongs(visibleSongs);

        return response;
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponse> getMyPlaylists() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return playlistRepository
                .findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream().map(playlistMapper::toPlaylistResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponse> getPublicPlaylistsByOwner(UUID ownerId) {
        if (!userRepository.existsById(ownerId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        return playlistRepository
                .findByOwnerIdAndVisibilityOrderByCreatedAtDesc(ownerId, Visibility.PUBLIC)
                .stream().map(playlistMapper::toPlaylistResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PlaylistResponse> searchPublicPlaylists(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String searchKeyword = keyword == null ? "" : keyword.trim();
        return playlistRepository
                .searchPublicPlaylists(searchKeyword, pageable)
                .map(playlistMapper::toPlaylistResponse);
    }

    @Transactional
    public PlaylistResponse updatePlaylist(UUID playlistId, PlaylistUpdateRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId).orElseThrow(() -> new AppException(ErrorCode.PLAYLIST_NOT_EXISTED));

        validatePermission(playlist);

        if (request != null) {
            playlistMapper.updatePlaylist(playlist, request);
        }

        return playlistMapper.toPlaylistResponse(playlistRepository.save(playlist));
    }

    @Transactional
    public void deletePlaylist(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId).orElseThrow(
                () -> new AppException(ErrorCode.PLAYLIST_NOT_EXISTED)
        );

        validatePermission(playlist);

        favoritePlaylistRepository.deleteAllByIdPlaylistId(playlistId);
        playlistRepository.delete(playlist);
    }

    private void validatePermission(Playlist playlist) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !playlist.getOwner().getEmail().equals(email)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private boolean canViewSong(Song song) {
        if (song.getVisibility() == Visibility.PUBLIC) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return isAdmin || song.getUploader().getEmail().equals(email);
    }
}

