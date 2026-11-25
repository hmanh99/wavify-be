package com.example.wavifybe.service;

import com.example.wavifybe.dto.request.FavoriteBatchCheckRequest;
import com.example.wavifybe.dto.response.FavoriteSongResponse;
import com.example.wavifybe.dto.response.FavoriteStatusResponse;
import com.example.wavifybe.dto.response.SongResponse;
import com.example.wavifybe.entity.*;
import com.example.wavifybe.exception.AppException;
import com.example.wavifybe.exception.ErrorCode;
import com.example.wavifybe.mapper.FavoriteMapper;
import com.example.wavifybe.repository.FavoriteSongRepository;
import com.example.wavifybe.repository.SongRepository;
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
class FavoriteSongServiceTest {

    @Mock
    FavoriteSongRepository favoriteSongRepository;
    @Mock
    SongRepository songRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    FavoriteMapper favoriteMapper;

    @InjectMocks
    FavoriteSongService favoriteSongService;

    private User currentUser;
    private User otherUser;
    private UUID songId;
    private Song publicReadySong;

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

        songId = UUID.randomUUID();
        publicReadySong = new Song();
        publicReadySong.setId(songId);
        publicReadySong.setTitle("Test Song");
        publicReadySong.setUploader(otherUser);
        publicReadySong.setVisibility(Visibility.PUBLIC);
        publicReadySong.setStatus(Status.READY);

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
    void addFavoriteSong_publicReadySong_succeeds() {
        Instant now = Instant.now();
        FavoriteSong saved = FavoriteSong.builder()
                .id(new FavoriteSongId(currentUser.getId(), songId))
                .user(currentUser)
                .song(publicReadySong)
                .createdAt(now)
                .build();
        FavoriteSongResponse expected = FavoriteSongResponse.builder()
                .song(SongResponse.builder().id(songId).title("Test Song").build())
                .favoritedAt(now)
                .build();

        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(songRepository.findById(songId)).thenReturn(Optional.of(publicReadySong));
        when(favoriteSongRepository.existsByIdUserIdAndIdSongId(currentUser.getId(), songId)).thenReturn(false);
        when(favoriteSongRepository.save(any(FavoriteSong.class))).thenReturn(saved);
        when(favoriteMapper.toFavoriteSongResponse(saved)).thenReturn(expected);

        FavoriteSongResponse result = favoriteSongService.addFavoriteSong(songId);

        assertThat(result).isEqualTo(expected);
        verify(favoriteSongRepository).save(any(FavoriteSong.class));
    }

    @Test
    void addFavoriteSong_songNotFound_throwsSongNotExisted() {
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(songRepository.findById(songId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteSongService.addFavoriteSong(songId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SONG_NOT_EXISTED);
    }

    @Test
    void addFavoriteSong_privateSongOfOtherUser_throwsUnauthorized() {
        publicReadySong.setVisibility(Visibility.PRIVATE);
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(songRepository.findById(songId)).thenReturn(Optional.of(publicReadySong));

        assertThatThrownBy(() -> favoriteSongService.addFavoriteSong(songId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void addFavoriteSong_privateSongOfOwner_succeeds() {
        publicReadySong.setVisibility(Visibility.PRIVATE);
        publicReadySong.setUploader(currentUser);
        FavoriteSong saved = FavoriteSong.builder()
                .id(new FavoriteSongId(currentUser.getId(), songId))
                .user(currentUser)
                .song(publicReadySong)
                .build();
        FavoriteSongResponse expected = FavoriteSongResponse.builder().build();

        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(songRepository.findById(songId)).thenReturn(Optional.of(publicReadySong));
        when(favoriteSongRepository.existsByIdUserIdAndIdSongId(currentUser.getId(), songId)).thenReturn(false);
        when(favoriteSongRepository.save(any(FavoriteSong.class))).thenReturn(saved);
        when(favoriteMapper.toFavoriteSongResponse(saved)).thenReturn(expected);

        assertThat(favoriteSongService.addFavoriteSong(songId)).isEqualTo(expected);
    }

    @Test
    void addFavoriteSong_alreadyFavorited_throwsConflict() {
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(songRepository.findById(songId)).thenReturn(Optional.of(publicReadySong));
        when(favoriteSongRepository.existsByIdUserIdAndIdSongId(currentUser.getId(), songId)).thenReturn(true);

        assertThatThrownBy(() -> favoriteSongService.addFavoriteSong(songId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SONG_ALREADY_FAVORITED);
    }

    @Test
    void removeFavoriteSong_succeeds() {
        FavoriteSong favoriteSong = FavoriteSong.builder()
                .id(new FavoriteSongId(currentUser.getId(), songId))
                .user(currentUser)
                .song(publicReadySong)
                .build();
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(favoriteSongRepository.findByIdUserIdAndIdSongId(currentUser.getId(), songId))
                .thenReturn(Optional.of(favoriteSong));

        favoriteSongService.removeFavoriteSong(songId);

        verify(favoriteSongRepository).delete(favoriteSong);
    }

    @Test
    void removeFavoriteSong_notFound_throwsFavoriteSongNotFound() {
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(favoriteSongRepository.findByIdUserIdAndIdSongId(currentUser.getId(), songId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteSongService.removeFavoriteSong(songId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FAVORITE_SONG_NOT_FOUND);
    }

    @Test
    void getMyFavoriteSongs_returnsPagedResponses() {
        FavoriteSong fs = FavoriteSong.builder()
                .id(new FavoriteSongId(currentUser.getId(), songId))
                .user(currentUser)
                .song(publicReadySong)
                .createdAt(Instant.now())
                .build();
        FavoriteSongResponse response = FavoriteSongResponse.builder()
                .song(SongResponse.builder().id(songId).build())
                .favoritedAt(fs.getCreatedAt())
                .build();

        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(favoriteSongRepository.findVisibleFavoritesByUserId(eq(currentUser.getId()), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fs)));
        when(favoriteMapper.toFavoriteSongResponse(fs)).thenReturn(response);

        Page<FavoriteSongResponse> page = favoriteSongService.getMyFavoriteSongs(0, 20);

        assertThat(page.getContent()).containsExactly(response);
    }

    @Test
    void checkFavoriteSong_returnsStatus() {
        Instant favoritedAt = Instant.now();
        FavoriteSong fs = FavoriteSong.builder().createdAt(favoritedAt).build();

        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(songRepository.existsById(songId)).thenReturn(true);
        when(favoriteSongRepository.findByIdUserIdAndIdSongId(currentUser.getId(), songId))
                .thenReturn(Optional.of(fs));

        FavoriteStatusResponse status = favoriteSongService.checkFavoriteSong(songId);

        assertThat(status.isFavorited()).isTrue();
        assertThat(status.getFavoritedAt()).isEqualTo(favoritedAt);
    }

    @Test
    void checkFavoriteSongs_batchCheckReturnsBooleanMap() {
        UUID songId2 = UUID.randomUUID();
        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(favoriteSongRepository.findFavoritedSongIds(eq(currentUser.getId()), anyCollection()))
                .thenReturn(Set.of(songId));

        Map<UUID, Boolean> result = favoriteSongService.checkFavoriteSongs(
                new FavoriteBatchCheckRequest(List.of(songId, songId2))
        );

        assertThat(result).containsExactly(Map.entry(songId, true), Map.entry(songId2, false));
    }
}
