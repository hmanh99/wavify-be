package com.example.wavifybe.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "playlist_songs",
        indexes = {
                @Index(name = "idx_playlist_songs_position", columnList = "playlist_id, position ASC"),
                @Index(name = "idx_playlist_songs_song_id", columnList = "song_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlaylistSong {
    @EmbeddedId
    PlaylistSongId id = new PlaylistSongId();

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("playlistId")
    @JoinColumn(name = "playlist_id")
    Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("songId")
    @JoinColumn(name = "song_id")
    Song song;

    @Column(nullable = false)
    int position;
}

