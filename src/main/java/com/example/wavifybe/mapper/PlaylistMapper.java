package com.example.wavifybe.mapper;

import com.example.wavifybe.dto.request.PlaylistCreationRequest;
import com.example.wavifybe.dto.request.PlaylistUpdateRequest;
import com.example.wavifybe.dto.response.PlaylistResponse;
import com.example.wavifybe.dto.response.PlaylistSongResponse;
import com.example.wavifybe.dto.response.SongResponse;
import com.example.wavifybe.entity.Playlist;
import com.example.wavifybe.entity.PlaylistSong;
import com.example.wavifybe.entity.Visibility;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {SongMapper.class})
public interface PlaylistMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "songs", ignore = true)
    Playlist toPlaylist(PlaylistCreationRequest request);

    @Mapping(target = "songs", source = "songs")
    PlaylistResponse toPlaylistResponse(Playlist playlist);

    @Mapping(target = ".", source = "song")
    @Mapping(target = "uploaderId", source = "song.uploader.id")
    SongResponse toSongResponse(PlaylistSong playlistSong);

    PlaylistSongResponse toPlaylistSongResponse(PlaylistSong playlistSong);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "songs", ignore = true)
    void updatePlaylist(@MappingTarget Playlist playlist, PlaylistUpdateRequest request);
}

