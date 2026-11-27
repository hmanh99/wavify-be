package com.example.wavifybe.mapper;

import com.example.wavifybe.dto.response.FavoritePlaylistResponse;
import com.example.wavifybe.dto.response.FavoriteSongResponse;
import com.example.wavifybe.entity.FavoritePlaylist;
import com.example.wavifybe.entity.FavoriteSong;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {SongMapper.class, PlaylistMapper.class})
public interface FavoriteMapper {

    @Mapping(target = "favoritedAt", source = "createdAt")
    @Mapping(target = "song", source = "song")
    FavoriteSongResponse toFavoriteSongResponse(FavoriteSong favoriteSong);

    @Mapping(target = "favoritedAt", source = "createdAt")
    @Mapping(target = "playlist", source = "playlist")
    FavoritePlaylistResponse toFavoritePlaylistResponse(FavoritePlaylist favoritePlaylist);
}
