package com.example.wavifybe.service;

import com.example.wavifybe.dto.request.AddSongToPlaylistRequest;
import com.example.wavifybe.dto.request.ReorderPlaylistSongsRequest;
import com.example.wavifybe.dto.response.PlaylistSongResponse;
import com.example.wavifybe.entity.*;
import com.example.wavifybe.exception.AppException;
import com.example.wavifybe.exception.ErrorCode;
import com.example.wavifybe.mapper.PlaylistMapper;
import com.example.wavifybe.repository.PlaylistRepository;
import com.example.wavifybe.repository.PlaylistSongRepository;
import com.example.wavifybe.repository.SongRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlaylistSongService {
    PlaylistRepository playlistRepository;
    PlaylistSongRepository playlistSongRepository;
    SongRepository songRepository;
    PlaylistMapper playlistMapper;

    @Transactional(readOnly = true)
    public List<PlaylistSongResponse> getPlaylistSongs(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAYLIST_NOT_EXISTED));

        if (playlist.getVisibility() == Visibility.PRIVATE) {
            validatePlaylistPermission(playlist);
        }

        ViewerContext viewer = ViewerContext.fromSecurityContext();
        return playlistSongRepository.findByIdPlaylistIdOrderByPositionAsc(playlistId).stream()
                .filter(ps -> viewer.canView(ps.getSong()))
                .map(playlistMapper::toPlaylistSongResponse)
                .toList();
    }

    @Transactional
    public List<PlaylistSongResponse> addSongsToPlaylist(UUID playlistId, AddSongToPlaylistRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAYLIST_NOT_EXISTED));

        validatePlaylistPermission(playlist);

        Set<UUID> uniqueSongIds = new LinkedHashSet<>(request.getSongIds());
        if (uniqueSongIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> existingSongIds = playlistSongRepository.findSongIdsByPlaylistIdAndSongIdIn(playlistId, uniqueSongIds);
        if (!existingSongIds.isEmpty()) {
            throw new AppException(ErrorCode.SONG_ALREADY_IN_PLAYLIST);
        }

        List<Song> songs = songRepository.findByIdIn(uniqueSongIds);
        Map<UUID, Song> songMap = songs.stream()
                .collect(Collectors.toMap(Song::getId, Function.identity()));

        int currentMaxPosition = playlistSongRepository.findMaxPosition(playlistId);
        List<PlaylistSong> newPlaylistSongs = new ArrayList<>(uniqueSongIds.size());

        for (UUID songId : uniqueSongIds) {
            Song song = songMap.get(songId);
            if (song == null) {
                throw new AppException(ErrorCode.SONG_NOT_EXISTED);
            }

            if (song.getVisibility() == Visibility.PRIVATE) {
                validateSongPermission(song);
            }

            PlaylistSong playlistSong = new PlaylistSong();
            playlistSong.setId(new PlaylistSongId(playlistId, songId));
            playlistSong.setPlaylist(playlist);
            playlistSong.setSong(song);
            playlistSong.setPosition(++currentMaxPosition);

            newPlaylistSongs.add(playlistSong);
        }

        return playlistSongRepository.saveAll(newPlaylistSongs).stream()
                .map(playlistMapper::toPlaylistSongResponse)
                .toList();
    }

    @Transactional
    public void removeSongFromPlaylist(UUID playlistId, UUID songId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAYLIST_NOT_EXISTED));

        validatePlaylistPermission(playlist);

        PlaylistSong playlistSong = playlistSongRepository.findByIdPlaylistIdAndIdSongId(playlistId, songId)
                .orElseThrow(() -> new AppException(ErrorCode.SONG_NOT_IN_PLAYLIST));

        int removedPosition = playlistSong.getPosition();

        playlistSongRepository.deleteByIdPlaylistIdAndIdSongId(playlistId, songId);
        playlistSongRepository.decrementPositionsAfter(playlistId, removedPosition);
    }

    @Transactional
    public List<PlaylistSongResponse> reorderPlaylistSongs(UUID playlistId, ReorderPlaylistSongsRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAYLIST_NOT_EXISTED));

        validatePlaylistPermission(playlist);

        List<PlaylistSong> currentSongs = playlistSongRepository.findByIdPlaylistIdOrderByPositionAsc(playlistId);
        List<UUID> newOrderIds = request.getSongIds();

        if (newOrderIds == null
                || newOrderIds.size() != currentSongs.size()
                || new HashSet<>(newOrderIds).size() != currentSongs.size()) {
            throw new AppException(ErrorCode.INVALID_PLAYLIST_REORDER);
        }

        Map<UUID, PlaylistSong> currentSongMap = currentSongs.stream()
                .collect(Collectors.toMap(ps -> ps.getId().getSongId(), Function.identity()));

        List<PlaylistSong> reorderedSongs = new ArrayList<>(newOrderIds.size());
        for (int i = 0; i < newOrderIds.size(); i++) {
            UUID songId = newOrderIds.get(i);
            PlaylistSong playlistSong = currentSongMap.get(songId);
            if (playlistSong == null) {
                throw new AppException(ErrorCode.INVALID_PLAYLIST_REORDER);
            }
            playlistSong.setPosition(i + 1);
            reorderedSongs.add(playlistSong);
        }

        return playlistSongRepository.saveAll(reorderedSongs).stream()
                .map(playlistMapper::toPlaylistSongResponse)
                .toList();
    }

    private void validatePlaylistPermission(Playlist playlist) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !playlist.getOwner().getEmail().equals(email)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateSongPermission(Song song) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !song.getUploader().getEmail().equals(email)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}

