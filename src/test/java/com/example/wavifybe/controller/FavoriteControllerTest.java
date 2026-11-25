package com.example.wavifybe.controller;

import com.example.wavifybe.configuration.SecurityConfig;
import com.example.wavifybe.dto.request.FavoriteBatchCheckRequest;
import com.example.wavifybe.dto.response.*;
import com.example.wavifybe.service.FavoritePlaylistService;
import com.example.wavifybe.service.FavoriteSongService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@Import(SecurityConfig.class)
class FavoriteControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    FavoriteSongService favoriteSongService;

    @MockitoBean
    FavoritePlaylistService favoritePlaylistService;

    @Test
    void getMyFavoriteSongs_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/favorites/songs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyFavoriteSongs_authenticated_returns200() throws Exception {
        UUID songId = UUID.randomUUID();
        FavoriteSongResponse item = FavoriteSongResponse.builder()
                .song(SongResponse.builder().id(songId).title("Song 1").build())
                .favoritedAt(Instant.now())
                .build();
        when(favoriteSongService.getMyFavoriteSongs(0, 20)).thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/favorites/songs").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.content[0].song.id").value(songId.toString()));
    }

    @Test
    void getMyFavoriteSongs_invalidPage_returns400() throws Exception {
        mockMvc.perform(get("/favorites/songs").param("page", "-1").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addFavoriteSong_returns201() throws Exception {
        UUID songId = UUID.randomUUID();
        FavoriteSongResponse response = FavoriteSongResponse.builder()
                .song(SongResponse.builder().id(songId).build())
                .favoritedAt(Instant.now())
                .build();
        when(favoriteSongService.addFavoriteSong(songId)).thenReturn(response);

        mockMvc.perform(post("/favorites/songs/{songId}", songId).with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.result.song.id").value(songId.toString()));
    }

    @Test
    void removeFavoriteSong_returns200() throws Exception {
        UUID songId = UUID.randomUUID();

        mockMvc.perform(delete("/favorites/songs/{songId}", songId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(favoriteSongService).removeFavoriteSong(songId);
    }

    @Test
    void checkFavoriteSong_returns200() throws Exception {
        UUID songId = UUID.randomUUID();
        when(favoriteSongService.checkFavoriteSong(songId))
                .thenReturn(FavoriteStatusResponse.builder().favorited(true).favoritedAt(Instant.now()).build());

        mockMvc.perform(get("/favorites/songs/{songId}/check", songId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.favorited").value(true));
    }

    @Test
    void checkFavoriteSongs_emptyIds_returns400() throws Exception {
        FavoriteBatchCheckRequest request = new FavoriteBatchCheckRequest(List.of());

        mockMvc.perform(post("/favorites/songs/check")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkFavoriteSongs_validIds_returns200() throws Exception {
        UUID songId = UUID.randomUUID();
        FavoriteBatchCheckRequest request = new FavoriteBatchCheckRequest(List.of(songId));
        when(favoriteSongService.checkFavoriteSongs(any())).thenReturn(Map.of(songId, true));

        mockMvc.perform(post("/favorites/songs/check")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result['" + songId + "']").value(true));
    }

    @Test
    void getMyFavoritePlaylists_authenticated_returns200() throws Exception {
        UUID playlistId = UUID.randomUUID();
        FavoritePlaylistResponse item = FavoritePlaylistResponse.builder()
                .playlist(PlaylistResponse.builder().id(playlistId).name("Mix").build())
                .favoritedAt(Instant.now())
                .build();
        when(favoritePlaylistService.getMyFavoritePlaylists(0, 20)).thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/favorites/playlists").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.content[0].playlist.id").value(playlistId.toString()));
    }

    @Test
    void addFavoritePlaylist_returns201() throws Exception {
        UUID playlistId = UUID.randomUUID();
        FavoritePlaylistResponse response = FavoritePlaylistResponse.builder()
                .playlist(PlaylistResponse.builder().id(playlistId).build())
                .favoritedAt(Instant.now())
                .build();
        when(favoritePlaylistService.addFavoritePlaylist(playlistId)).thenReturn(response);

        mockMvc.perform(post("/favorites/playlists/{playlistId}", playlistId).with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.result.playlist.id").value(playlistId.toString()));
    }

    @Test
    void removeFavoritePlaylist_returns200() throws Exception {
        UUID playlistId = UUID.randomUUID();

        mockMvc.perform(delete("/favorites/playlists/{playlistId}", playlistId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(favoritePlaylistService).removeFavoritePlaylist(playlistId);
    }

    @Test
    void checkFavoritePlaylist_returns200() throws Exception {
        UUID playlistId = UUID.randomUUID();
        when(favoritePlaylistService.checkFavoritePlaylist(playlistId))
                .thenReturn(FavoriteStatusResponse.builder().favorited(false).favoritedAt(null).build());

        mockMvc.perform(get("/favorites/playlists/{playlistId}/check", playlistId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.favorited").value(false));
    }

    @Test
    void checkFavoritePlaylists_validIds_returns200() throws Exception {
        UUID playlistId = UUID.randomUUID();
        FavoriteBatchCheckRequest request = new FavoriteBatchCheckRequest(List.of(playlistId));
        when(favoritePlaylistService.checkFavoritePlaylists(any())).thenReturn(Map.of(playlistId, true));

        mockMvc.perform(post("/favorites/playlists/check")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result['" + playlistId + "']").value(true));
    }
}
