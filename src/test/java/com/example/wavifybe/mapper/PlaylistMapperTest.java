package com.example.wavifybe.mapper;

import com.example.wavifybe.dto.response.FavoritePlaylistResponse;
import com.example.wavifybe.dto.response.PlaylistResponse;
import com.example.wavifybe.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlaylistMapperTest {

    private PlaylistMapperImpl playlistMapper;
    private FavoriteMapperImpl favoriteMapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        UserMapperImpl userMapper = new UserMapperImpl();
        SongMapperImpl songMapper = new SongMapperImpl();

        playlistMapper = new PlaylistMapperImpl();
        ReflectionTestUtils.setField(playlistMapper, "songMapper", songMapper);
        ReflectionTestUtils.setField(playlistMapper, "userMapper", userMapper);

        favoriteMapper = new FavoriteMapperImpl();
        ReflectionTestUtils.setField(favoriteMapper, "songMapper", songMapper);
        ReflectionTestUtils.setField(favoriteMapper, "playlistMapper", playlistMapper);

        objectMapper = new ObjectMapper();
    }

    @Test
    void toPlaylistResponse_ignoresSongsAndDoesNotTouchLazyCollection() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");
        owner.setUsername("owner");
        owner.setRole(UserRole.USER);
        owner.setCreatedAt(Instant.now());

        Playlist playlist = spy(new Playlist());
        playlist.setId(UUID.randomUUID());
        playlist.setName("Road Trip");
        playlist.setOwner(owner);
        playlist.setVisibility(Visibility.PUBLIC);
        playlist.setCreatedAt(Instant.now());

        PlaylistResponse response = playlistMapper.toPlaylistResponse(playlist);

        verify(playlist, never()).getSongs();
        assertThat(response.getSongs()).isNull();
        assertThat(response.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(response.getOwner().getEmail()).isEqualTo(owner.getEmail());
        assertThat(response.getOwner().getUsername()).isEqualTo(owner.getUsername());
        assertThat(response.getOwner().getRole()).isEqualTo(owner.getRole());
    }

    @Test
    void toFavoritePlaylistResponse_delegatesToPlaylistMapperWithoutTouchingSongs() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");
        owner.setUsername("owner");
        owner.setRole(UserRole.USER);

        Playlist playlist = spy(new Playlist());
        playlist.setId(UUID.randomUUID());
        playlist.setName("Favorites");
        playlist.setOwner(owner);
        playlist.setVisibility(Visibility.PUBLIC);

        FavoritePlaylist favoritePlaylist = FavoritePlaylist.builder()
                .id(new FavoritePlaylistId(owner.getId(), playlist.getId()))
                .user(owner)
                .playlist(playlist)
                .createdAt(Instant.now())
                .build();

        FavoritePlaylistResponse response = favoriteMapper.toFavoritePlaylistResponse(favoritePlaylist);

        verify(playlist, never()).getSongs();
        assertThat(response.getPlaylist().getId()).isEqualTo(playlist.getId());
        assertThat(response.getPlaylist().getSongs()).isNull();
    }

    @Test
    void playlistResponse_omitsSongsOnlyWhenNull() throws Exception {
        PlaylistResponse summary = PlaylistResponse.builder()
                .id(UUID.randomUUID())
                .name("Summary")
                .build();
        PlaylistResponse detailEmpty = PlaylistResponse.builder()
                .id(UUID.randomUUID())
                .name("Detail")
                .songs(List.of())
                .build();

        String summaryJson = objectMapper.writeValueAsString(summary);
        String detailJson = objectMapper.writeValueAsString(detailEmpty);

        assertThat(summaryJson).doesNotContain("\"songs\"");
        assertThat(detailJson).contains("\"songs\":[]");
    }
}
