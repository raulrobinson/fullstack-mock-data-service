package com.raulbolivar.driven.jsonplaceholder.mapper;

import com.raulbolivar.driven.jsonplaceholder.dto.PostDto;
import com.raulbolivar.driven.jsonplaceholder.dto.UserDto;
import com.raulbolivar.model.Post;
import com.raulbolivar.model.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface JSONPlaceHolderMapper {

    Post toDomainPost(PostDto postDto);

    User toDomainUser(UserDto userDto);
}
