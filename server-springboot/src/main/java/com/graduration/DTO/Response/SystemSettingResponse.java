package com.graduration.DTO.Response;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SystemSettingResponse {
    boolean studentTopicProposalEnabled;
    int auditRetentionLimit;
    boolean libraryTopicsVisible;
    boolean templatesVisible;
    Instant updatedAt;
}
