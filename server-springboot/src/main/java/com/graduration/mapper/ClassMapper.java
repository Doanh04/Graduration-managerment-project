package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.graduration.DTO.Request.ClassRequest;
import com.graduration.DTO.Response.ClassResponse;
import com.graduration.entity.ClassEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ClassMapper {
    @Mapping(source = "nameClass", target = "className")
    @Mapping(target = "classId", ignore = true)
    @Mapping(target = "major", ignore = true)
    @Mapping(target = "student", ignore = true)
    ClassEntity toClassEntity(ClassRequest request);

    @Mapping(source = "classId", target = "idClass")
    @Mapping(source = "major.majorId", target = "majorId")
    @Mapping(source = "major.majorName", target = "majorName")
    ClassResponse toClassResponse(ClassEntity classEntity);
}
