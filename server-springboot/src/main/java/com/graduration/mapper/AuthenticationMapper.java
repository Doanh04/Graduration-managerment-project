package com.graduration.mapper;

import org.mapstruct.Mapper;

import com.graduration.DTO.Request.AuthenticationRequest;

@Mapper(componentModel = "spring")
public interface AuthenticationMapper {
    default String toLoginIdentifier(AuthenticationRequest request) {
        if (request == null) {
            return null;
        }

        String[] identifiers = {
            request.getIdentifier(), request.getUserName(), request.getLecturerCode(), request.getStudentCode()
        };
        String resolved = null;
        for (String identifier : identifiers) {
            if (identifier == null || identifier.isBlank()) {
                continue;
            }
            String normalized = identifier.trim();
            if (resolved != null && !resolved.equals(normalized)) {
                return null;
            }
            resolved = normalized;
        }
        return resolved;
    }
}
