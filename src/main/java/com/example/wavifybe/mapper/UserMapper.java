package com.example.wavifybe.mapper;

import com.example.wavifybe.dto.request.RegisterRequest;
import com.example.wavifybe.dto.request.UserUpdateRequest;
import com.example.wavifybe.dto.response.UserResponse;
import com.example.wavifybe.entity.User;
import org.mapstruct.*;
import org.springframework.context.annotation.Bean;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(RegisterRequest request);

    UserResponse toUserResponse(User user);

    @Mapping(target = "role", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
