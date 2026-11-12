package com.example.wavifybe.mapper;

import com.example.wavifybe.dto.request.SongCreationRequest;
import com.example.wavifybe.dto.request.SongUpdateRequest;
import com.example.wavifybe.dto.response.SongResponse;
import com.example.wavifybe.entity.Song;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SongMapper {
    Song toSong(SongCreationRequest request);

    @Mapping(target = "uploaderId", source = "uploader.id")
    SongResponse toSongResponse(Song song);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSong(@MappingTarget Song song, SongUpdateRequest request);
}
