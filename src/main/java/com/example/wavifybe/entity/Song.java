package com.example.wavifybe.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "songs", indexes = @Index(name = "idx_songs_uploader_created", columnList = "uploader_id, created_at DESC"))
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "artist")
    String artist;

    @Column(name = "album")
    String album;

    @Column(name = "duration_sec")
    int durationSec;

    /// cloudinary url of radio file
    @Column(name = "file_url", nullable = false)
    String fileUrl;

    /// cloudinary url of cover photo
    @Column(name = "cover_url", nullable = false)
    String coverUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    User uploader;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    Status status = Status.PROCESSING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
}
