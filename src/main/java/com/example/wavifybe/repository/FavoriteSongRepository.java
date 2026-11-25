package com.example.wavifybe.repository;

import com.example.wavifybe.entity.FavoriteSong;
import com.example.wavifybe.entity.FavoriteSongId;
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
public interface FavoriteSongRepository extends JpaRepository<FavoriteSong, FavoriteSongId> {

    boolean existsByIdUserIdAndIdSongId(UUID userId, UUID songId);

    Optional<FavoriteSong> findByIdUserIdAndIdSongId(UUID userId, UUID songId);

    @EntityGraph(attributePaths = {"song", "song.uploader"})
    @Query("""
            SELECT fs FROM FavoriteSong fs
            JOIN fs.song s
            WHERE fs.id.userId = :userId
              AND (
                  (s.visibility = com.example.wavifybe.entity.Visibility.PUBLIC
                      AND s.status = com.example.wavifybe.entity.Status.READY)
                  OR s.uploader.id = :userId
                  OR :isAdmin = true
              )
            """)
    Page<FavoriteSong> findVisibleFavoritesByUserId(
            @Param("userId") UUID userId,
            @Param("isAdmin") boolean isAdmin,
            Pageable pageable
    );

    @Query("""
            SELECT fs.id.songId FROM FavoriteSong fs
            WHERE fs.id.userId = :userId AND fs.id.songId IN :songIds
            """)
    Set<UUID> findFavoritedSongIds(
            @Param("userId") UUID userId,
            @Param("songIds") Collection<UUID> songIds
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM FavoriteSong fs
            WHERE fs.id.songId = :songId
            """)
    void deleteAllByIdSongId(@Param("songId") UUID songId);
}
