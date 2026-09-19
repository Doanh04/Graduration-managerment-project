package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.graduration.DTO.Response.TemplateResponse;
import com.graduration.entity.TemplateEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TemplateMapper {
    @Mapping(source = "uploadedBy.userId", target = "uploadedByUserId")
    @Mapping(source = "uploadedBy.userName", target = "uploadedByUsername")
    @Mapping(source = "originalFileName", target = "fileName")
    TemplateResponse toTemplateResponse(TemplateEntity template);
}
