package com.example.wavifybe.repository;

import com.example.wavifybe.entity.PlayHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayHistoryRepository extends JpaRepository<PlayHistory, UUID> {

    /// Fetch play history of user with song + uploader details
    @EntityGraph(attributePaths = {"song", "song.uploader"})
    Page<PlayHistory> findByUserId(UUID userId, Pageable pageable);

    /// Find the most recent play of a song by a user
    Optional<PlayHistory> findFirstByUserIdAndSongIdOrderByPlayedAtDesc(UUID userId, UUID songId);

    /// Delete a single history of a user
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM PlayHistory ph
            WHERE ph.id = :id AND ph.user.id = :userId
            """)
    void deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    /// Clear entire user play history
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM PlayHistory ph
            WHERE ph.user.id = :userId
            """)
    void deleteAllByUserId(@Param("userId") UUID userId);
}
