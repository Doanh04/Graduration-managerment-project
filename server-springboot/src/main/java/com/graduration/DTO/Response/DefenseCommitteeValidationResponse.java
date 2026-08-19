package com.graduration.DTO.Response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DefenseCommitteeValidationResponse {
    boolean valid;
    long activeMemberCount;
    long chairpersonCount;
    long secretaryCount;
    long reviewerCount;
    List<String> errors;
}
