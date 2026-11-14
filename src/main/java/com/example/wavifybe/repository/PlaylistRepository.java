package com.example.wavifybe.repository;

import com.example.wavifybe.entity.Playlist;
import com.example.wavifybe.entity.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {
    @EntityGraph(attributePaths = {"owner"})
    List<Playlist> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = {"owner"})
    List<Playlist> findByOwnerIdAndVisibilityOrderByCreatedAtDesc(UUID ownerId, Visibility visibility);

    Optional<Playlist> findByIdAndOwnerId(UUID id, UUID ownerId);

    /// Fetch playlist, owner, and songs, avoid N+1 query
    @Query("""
            SELECT p FROM Playlist p
            JOIN FETCH p.owner
            LEFT JOIN FETCH p.songs ps
            LEFT JOIN FETCH ps.song s
            LEFT JOIN FETCH s.uploader
            WHERE p.id = :id
            ORDER BY ps.position ASC
            """)
    Optional<Playlist> findDetailById(@Param("id") UUID id);


    @EntityGraph(attributePaths = {"owner"})
    @Query("""
            SELECT p FROM Playlist p
            WHERE p.visibility = 'PUBLIC'
              AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Playlist> searchPublicPlaylists(@Param("keyword") String keyword, Pageable pageable);
}
