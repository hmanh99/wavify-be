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
        name = "favorite_songs",
        indexes = {
                @Index(name = "idx_favorite_songs_user_created", columnList = "user_id, created_at DESC"),
                @Index(name = "idx_favorite_songs_song_id", columnList = "song_id")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FavoriteSong {
    @EmbeddedId
    @Builder.Default
    FavoriteSongId id = new FavoriteSongId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("songId")
    @JoinColumn(name = "song_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Song song;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
}
