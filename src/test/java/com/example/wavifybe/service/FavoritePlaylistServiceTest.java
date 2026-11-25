package com.example.wavifybe.service;

import com.example.wavifybe.dto.request.FavoriteBatchCheckRequest;
import com.example.wavifybe.dto.response.FavoritePlaylistResponse;
import com.example.wavifybe.dto.response.FavoriteStatusResponse;
import com.example.wavifybe.dto.response.PlaylistResponse;
import com.example.wavifybe.entity.*;
import com.example.wavifybe.exception.AppException;
import com.example.wavifybe.exception.ErrorCode;
import com.example.wavifybe.mapper.FavoriteMapper;
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

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoritePlaylistServiceTest {

    @Mock
    FavoritePlaylistRepository favoritePlaylistRepository;
    @Mock
    PlaylistRepository playlistRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    FavoriteMapper favoriteMapper;

    @InjectMocks
    FavoritePlaylistService favoritePlaylistService;

    private User currentUser;
    private User otherUser;
    private UUID playlistId;
    private Playlist publicPlaylist;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setEmail("user@example.com");
        currentUser.setRole(UserRole.USER);

        otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setEmail("other@example.com");
        otherUser.setRole(UserRole.USER);

        playlistId = UUID.randomUUID();
        publicPlaylist = new Playlist();
        publicPlaylist.setId(playlistId);
        publicPlaylist.setName("Chill Mix");
        publicPlaylist.setOwner(otherUser);
        publicPlaylist.setVisibility(Visibility.PUBLIC);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        currentUser.getEmail(),
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addFavoritePlaylist_publicPlaylist_succeeds() {
        Instant now = Instant.now();
        FavoritePlaylist saved = FavoritePlaylist.builder()
                .id(new FavoritePlaylistId(currentUser.getId(), playlistId))
                .user(currentUser)
                .playlist(publicPlaylist)
                .createdAt(now)
                .build();
        FavoritePlaylistResponse expected = FavoritePlaylistResponse.builder()
                .playlist(PlaylistResponse.builder().id(playlistId).name("Chill Mix").build())
                .favoritedAt(now)
                .build();

        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(publicPlaylist));
        when(favoritePlaylistRepository.existsByIdUserIdAndIdPlaylistId(currentUser.getId(), playlistId)).thenReturn(false);
        when(favoritePlaylistRepository.save(any(FavoritePlaylist.class))).thenReturn(saved);
        when(favoriteMapper.toFavoritePlaylistResponse(saved)).thenReturn(expected);

        FavoritePlaylistResponse result = favoritePlaylistService.addFavoritePlaylist(playlistId);

        assertThat(result).isEqualTo(expected);
        verify(favoritePlaylistRepository).save(any(FavoritePlaylist.class));
    }

    @Test
    void addFavoritePlaylist_notFound_throwsPlaylistNotExisted() {
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritePlaylistService.addFavoritePlaylist(playlistId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAYLIST_NOT_EXISTED);
    }

    @Test
    void addFavoritePlaylist_privatePlaylistOfOtherUser_throwsUnauthorized() {
        publicPlaylist.setVisibility(Visibility.PRIVATE);
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(publicPlaylist));

        assertThatThrownBy(() -> favoritePlaylistService.addFavoritePlaylist(playlistId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void addFavoritePlaylist_alreadyFavorited_throwsConflict() {
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(publicPlaylist));
        when(favoritePlaylistRepository.existsByIdUserIdAndIdPlaylistId(currentUser.getId(), playlistId)).thenReturn(true);

        assertThatThrownBy(() -> favoritePlaylistService.addFavoritePlaylist(playlistId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLAYLIST_ALREADY_FAVORITED);
    }

    @Test
    void removeFavoritePlaylist_succeeds() {
        FavoritePlaylist favoritePlaylist = FavoritePlaylist.builder()
                .id(new FavoritePlaylistId(currentUser.getId(), playlistId))
                .user(currentUser)
                .playlist(publicPlaylist)
                .build();
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(favoritePlaylistRepository.findByIdUserIdAndIdPlaylistId(currentUser.getId(), playlistId))
                .thenReturn(Optional.of(favoritePlaylist));

        favoritePlaylistService.removeFavoritePlaylist(playlistId);

        verify(favoritePlaylistRepository).delete(favoritePlaylist);
    }

    @Test
    void removeFavoritePlaylist_notFound_throwsFavoritePlaylistNotFound() {
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(favoritePlaylistRepository.findByIdUserIdAndIdPlaylistId(currentUser.getId(), playlistId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoritePlaylistService.removeFavoritePlaylist(playlistId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FAVORITE_PLAYLIST_NOT_FOUND);
    }

    @Test
    void getMyFavoritePlaylists_returnsPagedResponses() {
        FavoritePlaylist fp = FavoritePlaylist.builder()
                .id(new FavoritePlaylistId(currentUser.getId(), playlistId))
                .user(currentUser)
                .playlist(publicPlaylist)
                .createdAt(Instant.now())
                .build();
        FavoritePlaylistResponse response = FavoritePlaylistResponse.builder()
                .playlist(PlaylistResponse.builder().id(playlistId).build())
                .favoritedAt(fp.getCreatedAt())
                .build();

        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(favoritePlaylistRepository.findVisibleFavoritesByUserId(eq(currentUser.getId()), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fp)));
        when(favoriteMapper.toFavoritePlaylistResponse(fp)).thenReturn(response);

        Page<FavoritePlaylistResponse> page = favoritePlaylistService.getMyFavoritePlaylists(0, 20);

        assertThat(page.getContent()).containsExactly(response);
    }

    @Test
    void checkFavoritePlaylist_returnsStatus() {
        Instant favoritedAt = Instant.now();
        FavoritePlaylist fp = FavoritePlaylist.builder().createdAt(favoritedAt).build();

        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(playlistRepository.existsById(playlistId)).thenReturn(true);
        when(favoritePlaylistRepository.findByIdUserIdAndIdPlaylistId(currentUser.getId(), playlistId))
                .thenReturn(Optional.of(fp));

        FavoriteStatusResponse status = favoritePlaylistService.checkFavoritePlaylist(playlistId);

        assertThat(status.isFavorited()).isTrue();
        assertThat(status.getFavoritedAt()).isEqualTo(favoritedAt);
    }

    @Test
    void checkFavoritePlaylists_batchCheckReturnsBooleanMap() {
        UUID playlistId2 = UUID.randomUUID();
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(favoritePlaylistRepository.findFavoritedPlaylistIds(eq(currentUser.getId()), anyCollection()))
                .thenReturn(Set.of(playlistId));

        Map<UUID, Boolean> result = favoritePlaylistService.checkFavoritePlaylists(
                new FavoriteBatchCheckRequest(List.of(playlistId, playlistId2))
        );

        assertThat(result).containsExactly(Map.entry(playlistId, true), Map.entry(playlistId2, false));
    }
}
