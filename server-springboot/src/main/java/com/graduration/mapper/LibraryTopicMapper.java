package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.graduration.DTO.Request.LibraryTopicRequest;
import com.graduration.DTO.Response.LibraryTopicResponse;
import com.graduration.entity.LibraryTopicEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface LibraryTopicMapper {
    @Mapping(target = "idLibraryTopic", ignore = true)
    LibraryTopicEntity toLibraryTopicEntity(LibraryTopicRequest request);

    @Mapping(target = "idLibraryTopic", ignore = true)
    void updateLibraryTopic(LibraryTopicRequest request, @MappingTarget LibraryTopicEntity libraryTopic);

    LibraryTopicResponse toLibraryTopicResponse(LibraryTopicEntity libraryTopic);
}
