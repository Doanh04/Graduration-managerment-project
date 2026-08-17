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
    @Mapping(target = "templateType", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "originalFileName", ignore = true)
    @Mapping(target = "contentType", ignore = true)
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    @Mapping(target = "defensePeriod", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TemplateEntity toTemplateEntity(TemplateRequest request);

    @Mapping(target = "templateId", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "templateType", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "originalFileName", ignore = true)
    @Mapping(target = "contentType", ignore = true)
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    @Mapping(target = "defensePeriod", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateTemplate(TemplateRequest request, @MappingTarget TemplateEntity template);

    TemplateResponse toTemplateResponse(TemplateEntity template);
}
