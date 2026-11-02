package com.example.wavifybe.mapper;

import com.example.wavifybe.dto.request.RegisterRequest;
import com.example.wavifybe.dto.request.UserUpdateRequest;
import com.example.wavifybe.dto.response.UserResponse;
import com.example.wavifybe.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(RegisterRequest request);

    UserResponse toUserResponse(User user);

    @Mapping(target = "role", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
