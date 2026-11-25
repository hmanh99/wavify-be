package com.example.wavifybe.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "favorite_playlists",
        indexes = {
                @Index(name = "idx_favorite_playlists_user_created", columnList = "user_id, created_at DESC"),
                @Index(name = "idx_favorite_playlists_playlist_id", columnList = "playlist_id")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FavoritePlaylist {
    @EmbeddedId
    @Builder.Default
    FavoritePlaylistId id = new FavoritePlaylistId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("playlistId")
    @JoinColumn(name = "playlist_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Playlist playlist;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
}
