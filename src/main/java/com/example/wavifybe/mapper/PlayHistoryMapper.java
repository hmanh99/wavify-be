package com.example.wavifybe.mapper;

import com.example.wavifybe.dto.response.PlayHistoryResponse;
import com.example.wavifybe.entity.PlayHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {SongMapper.class})
public interface PlayHistoryMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "song", source = "song")
    PlayHistoryResponse toPlayHistoryResponse(PlayHistory playHistory);
}

