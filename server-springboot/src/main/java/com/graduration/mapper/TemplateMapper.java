package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.graduration.DTO.Request.TemplateRequest;
import com.graduration.DTO.Response.TemplateResponse;
import com.graduration.entity.TemplateEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TemplateMapper {
    @Mapping(target = "templateId", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    TemplateEntity toTemplateEntity(TemplateRequest request);

    @Mapping(target = "templateId", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    void updateTemplate(TemplateRequest request, @MappingTarget TemplateEntity template);

    TemplateResponse toTemplateResponse(TemplateEntity template);
}
