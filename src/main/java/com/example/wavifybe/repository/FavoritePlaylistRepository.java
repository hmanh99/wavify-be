package com.example.wavifybe.repository;

import com.example.wavifybe.entity.FavoritePlaylist;
import com.example.wavifybe.entity.FavoritePlaylistId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface FavoritePlaylistRepository extends JpaRepository<FavoritePlaylist, FavoritePlaylistId> {

    boolean existsByIdUserIdAndIdPlaylistId(UUID userId, UUID playlistId);

    Optional<FavoritePlaylist> findByIdUserIdAndIdPlaylistId(UUID userId, UUID playlistId);

    @EntityGraph(attributePaths = {"playlist", "playlist.owner"})
    @Query("""
            SELECT fp FROM FavoritePlaylist fp
            JOIN fp.playlist p
            WHERE fp.id.userId = :userId
              AND (
                  p.visibility = com.example.wavifybe.entity.Visibility.PUBLIC
                  OR p.owner.id = :userId
                  OR :isAdmin = true
              )
            """)
    Page<FavoritePlaylist> findVisibleFavoritesByUserId(
            @Param("userId") UUID userId,
            @Param("isAdmin") boolean isAdmin,
            Pageable pageable
    );

    @Query("""
            SELECT fp.id.playlistId FROM FavoritePlaylist fp
            WHERE fp.id.userId = :userId AND fp.id.playlistId IN :playlistIds
            """)
    Set<UUID> findFavoritedPlaylistIds(
            @Param("userId") UUID userId,
            @Param("playlistIds") Collection<UUID> playlistIds
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM FavoritePlaylist fp
            WHERE fp.id.playlistId = :playlistId
            """)
    void deleteAllByIdPlaylistId(@Param("playlistId") UUID playlistId);
}
