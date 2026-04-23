package com.raulbolivar.fs.infrastructure.entrypoints.mappers;

import com.raulbolivar.fs.domain.model.ApiMockRuntimeInfo;
import com.raulbolivar.fs.domain.model.UserMockDefinition;
import com.raulbolivar.fs.infrastructure.entrypoints.dto.ApiMockRuntimeDto;
import com.raulbolivar.fs.infrastructure.entrypoints.dto.UserMockDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMockDto toDto(UserMockDefinition definition);

    List<UserMockDto> toDtoList(List<UserMockDefinition> definitions);

    UserMockDefinition toDomain(UserMockDto dto);

    ApiMockRuntimeDto toDto(ApiMockRuntimeInfo info);
}
