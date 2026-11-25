package com.example.wavifybe.mapper;

import com.example.wavifybe.dto.response.FavoritePlaylistResponse;
import com.example.wavifybe.dto.response.FavoriteSongResponse;
import com.example.wavifybe.dto.response.PlaylistResponse;
import com.example.wavifybe.entity.FavoritePlaylist;
import com.example.wavifybe.entity.FavoriteSong;
import com.example.wavifybe.entity.Playlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {SongMapper.class, UserMapper.class})
public interface FavoriteMapper {

    @Mapping(target = "favoritedAt", source = "createdAt")
    @Mapping(target = "song", source = "song")
    FavoriteSongResponse toFavoriteSongResponse(FavoriteSong favoriteSong);

    @Mapping(target = "favoritedAt", source = "createdAt")
    @Mapping(target = "playlist", source = "playlist", qualifiedByName = "toPlaylistSummary")
    FavoritePlaylistResponse toFavoritePlaylistResponse(FavoritePlaylist favoritePlaylist);

    @Named("toPlaylistSummary")
    @Mapping(target = "songs", ignore = true)
    PlaylistResponse toPlaylistSummary(Playlist playlist);
}
