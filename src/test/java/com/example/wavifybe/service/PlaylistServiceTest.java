package com.example.wavifybe.service;

import com.example.wavifybe.dto.response.PlaylistResponse;
import com.example.wavifybe.dto.response.SongResponse;
import com.example.wavifybe.entity.*;
import com.example.wavifybe.mapper.PlaylistMapper;
import com.example.wavifybe.repository.FavoritePlaylistRepository;
import com.example.wavifybe.repository.PlaylistRepository;
import com.example.wavifybe.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    PlaylistRepository playlistRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    FavoritePlaylistRepository favoritePlaylistRepository;
    @Mock
    PlaylistMapper playlistMapper;

    @InjectMocks
    PlaylistService playlistService;

    private User owner;
    private User otherUser;
    private Playlist playlist;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");
        owner.setRole(UserRole.USER);

        otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setEmail("other@example.com");
        otherUser.setRole(UserRole.USER);

        playlist = new Playlist();
        playlist.setId(UUID.randomUUID());
        playlist.setName("Mix");
        playlist.setOwner(owner);
        playlist.setVisibility(Visibility.PUBLIC);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPlaylist_filtersPrivateSongsForOtherUsersAndMapsSongsOnce() {
        Song publicSong = new Song();
        publicSong.setId(UUID.randomUUID());
        publicSong.setTitle("Public Song");
        publicSong.setVisibility(Visibility.PUBLIC);
        publicSong.setUploader(owner);

        Song privateSong = new Song();
        privateSong.setId(UUID.randomUUID());
        privateSong.setTitle("Private Song");
        privateSong.setVisibility(Visibility.PRIVATE);
        privateSong.setUploader(owner);

        PlaylistSong ps1 = new PlaylistSong();
        ps1.setSong(publicSong);
        ps1.setPosition(1);

        PlaylistSong ps2 = new PlaylistSong();
        ps2.setSong(privateSong);
        ps2.setPosition(2);

        playlist.setSongs(List.of(ps1, ps2));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        otherUser.getEmail(),
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        PlaylistResponse baseResponse = PlaylistResponse.builder().id(playlist.getId()).name("Mix").build();
        SongResponse publicSongResponse = SongResponse.builder().id(publicSong.getId()).title("Public Song").build();

        when(playlistRepository.findDetailById(playlist.getId())).thenReturn(Optional.of(playlist));
        when(playlistMapper.toPlaylistResponse(playlist)).thenReturn(baseResponse);
        when(playlistMapper.toSongResponse(ps1)).thenReturn(publicSongResponse);

        PlaylistResponse result = playlistService.getPlaylist(playlist.getId());

        assertThat(result.getSongs()).containsExactly(publicSongResponse);
        verify(playlistMapper, times(1)).toSongResponse(ps1);
        verify(playlistMapper, never()).toSongResponse(ps2);
    }

    @Test
    void getPlaylist_allowsAdminToViewPrivateSongs() {
        Song privateSong = new Song();
        privateSong.setId(UUID.randomUUID());
        privateSong.setVisibility(Visibility.PRIVATE);
        privateSong.setUploader(owner);

        PlaylistSong ps = new PlaylistSong();
        ps.setSong(privateSong);
        ps.setPosition(1);
        playlist.setSongs(List.of(ps));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        PlaylistResponse baseResponse = PlaylistResponse.builder().id(playlist.getId()).build();
        SongResponse privateSongResponse = SongResponse.builder().id(privateSong.getId()).build();

        when(playlistRepository.findDetailById(playlist.getId())).thenReturn(Optional.of(playlist));
        when(playlistMapper.toPlaylistResponse(playlist)).thenReturn(baseResponse);
        when(playlistMapper.toSongResponse(ps)).thenReturn(privateSongResponse);

        PlaylistResponse result = playlistService.getPlaylist(playlist.getId());

        assertThat(result.getSongs()).containsExactly(privateSongResponse);
    }

    @Test
    void searchPublicPlaylists_returnsMappedPage() {
        PlaylistResponse summary = PlaylistResponse.builder().id(playlist.getId()).name("Mix").build();
        when(playlistRepository.searchPublicPlaylists(eq("mix"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(playlist)));
        when(playlistMapper.toPlaylistResponse(playlist)).thenReturn(summary);

        Page<PlaylistResponse> result = playlistService.searchPublicPlaylists("  mix ", 0, 20);

        assertThat(result.getContent()).containsExactly(summary);
        verify(playlistMapper, never()).toSongResponse(any());
    }
}
