package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeactivateCommitteeMemberRequest {
    @NotBlank(message = "COMMITTEE_MEMBER_DEACTIVATION_REASON_NOT_BLANK")
    String reason;
}
