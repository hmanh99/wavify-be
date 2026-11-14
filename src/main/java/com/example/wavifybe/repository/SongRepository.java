package com.example.wavifybe.repository;

import com.example.wavifybe.entity.Song;
import com.example.wavifybe.entity.Status;
import com.example.wavifybe.entity.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SongRepository extends JpaRepository<Song, UUID> {
    List<Song> findByUploaderId(UUID uploaderId);

    Page<Song> findByVisibilityAndStatus(Visibility visibility, Status status, Pageable pageable);

    /// Fetch Songs + uploaders
    @EntityGraph(attributePaths = { "uploader" })
    List<Song> findByIdIn(Collection<UUID> ids);

    /// Fetch by keyword
    @Query("""
            SELECT s FROM Song s
            WHERE s.visibility = 'PUBLIC' AND s.status = 'READY'
              AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Song> search(@Param("keyword") String keyword, Pageable pageable);
}
