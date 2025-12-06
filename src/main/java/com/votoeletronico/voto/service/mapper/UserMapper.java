package com.votoeletronico.voto.service.mapper;

import com.votoeletronico.voto.domain.user.User;
import com.votoeletronico.voto.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for User entity
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "lastLoginAt", source = "lastLoginAt")
    UserResponse toResponse(User user);
}
