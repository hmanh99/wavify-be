package com.example.wavifybe.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "playlists",
        indexes = @Index(name = "idx_playlists_owner_created", columnList = "owner_id, created_at DESC")
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Playlist {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    User owner;

    @Column(nullable = false)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    Visibility visibility = Visibility.PRIVATE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    List<PlaylistSong> songs = new ArrayList<>();
}
