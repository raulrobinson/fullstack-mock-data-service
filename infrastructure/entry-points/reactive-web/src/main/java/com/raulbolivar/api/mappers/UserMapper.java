package com.raulbolivar.api.mappers;

import com.raulbolivar.api.dto.ApiMockRuntimeDto;
import com.raulbolivar.api.dto.UserMockDto;
import com.raulbolivar.model.ApiMockRuntimeInfo;
import com.raulbolivar.model.UserMockDefinition;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMockDto toDto(UserMockDefinition definition);

    List<UserMockDto> toDtoList(List<UserMockDefinition> definitions);

    UserMockDefinition toDomain(UserMockDto dto);

    ApiMockRuntimeDto toDto(ApiMockRuntimeInfo info);
}
