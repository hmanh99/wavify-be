package com.example.wavifybe.repository;

import com.example.wavifybe.entity.PlaylistSong;
import com.example.wavifybe.entity.PlaylistSongId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, PlaylistSongId> {
    @EntityGraph(attributePaths = {"song", "song.uploader"})
    List<PlaylistSong> findByIdPlaylistIdOrderByPositionAsc(UUID playlistId);

    Optional<PlaylistSong> findByIdPlaylistIdAndIdSongId(UUID playlistId, UUID songId);

    /// find existed songs in playlist
    @Query("""
            SELECT ps.id.songId FROM PlaylistSong ps 
            WHERE ps.id.playlistId = :playlistId AND ps.id.songId IN :songIds
            """)
    Set<UUID> findSongIdsByPlaylistIdAndSongIdIn(
            @Param("playlistId") UUID playlistId,
            @Param("songIds") Collection<UUID> songIds
    );

    // find max position in playlist for add song
    @Query("""
            SELECT COALESCE(MAX(ps.position), 0) FROM PlaylistSong ps 
            WHERE ps.id.playlistId = :playlistId
            """)
    int findMaxPosition(@Param("playlistId") UUID playlistId);

    // delete playlist_song
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM PlaylistSong ps 
            WHERE ps.id.playlistId = :playlistId AND ps.id.songId = :songId
            """)
    void deleteByIdPlaylistIdAndIdSongId(@Param("playlistId") UUID playlistId, @Param("songId") UUID songId);


    // decrease playlist_song position
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PlaylistSong ps
            SET ps.position = ps.position - 1
            WHERE ps.id.playlistId = :playlistId AND ps.position > :removedPosition
            """)
    void decrementPositionsAfter(@Param("playlistId") UUID playlistId, @Param("removedPosition") int removedPosition);
}
