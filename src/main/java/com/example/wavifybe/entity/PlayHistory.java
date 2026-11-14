package com.example.wavifybe.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "play_history",
        indexes = {
            @Index(name = "idx_history_user_played_at", columnList = "user_id, played_at DESC"),
            @Index(name = "idx_history_song_played_at", columnList = "song_id, played_at DESC")
        }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    Song song;

    @CreationTimestamp
    @Column(name = "played_at", nullable = false, updatable = false)
    Instant playedAt;
}
