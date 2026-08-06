package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.graduration.DTO.Request.MajorRequest;
import com.graduration.DTO.Response.MajorResponse;
import com.graduration.entity.MajorEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MajorMapper {
    @Mapping(target = "majorId", ignore = true)
    @Mapping(target = "classEntity", ignore = true)
    MajorEntity toMajorEntity(MajorRequest request);

    MajorResponse toMajorResponse(MajorEntity major);
}
