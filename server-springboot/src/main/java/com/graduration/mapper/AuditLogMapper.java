package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import com.graduration.DTO.Response.AuditLogResponse;
import com.graduration.entity.AuditLogDocument;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AuditLogMapper {
    AuditLogResponse toAuditLogResponse(AuditLogDocument auditLog);
}
