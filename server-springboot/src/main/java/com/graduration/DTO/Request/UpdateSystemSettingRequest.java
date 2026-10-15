package com.graduration.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSystemSettingRequest {
    @NotNull
    Boolean studentTopicProposalEnabled;

    @NotNull
    @Min(1)
    Integer auditRetentionLimit;

    @NotNull
    Boolean libraryTopicsVisible;

    @NotNull
    Boolean templatesVisible;
}
