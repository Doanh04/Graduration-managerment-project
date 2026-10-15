package com.graduration.DTO.Request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitteeScoreSheetRequest {
    @Valid
    @NotEmpty(message = "SCORE_VALUE_INVALID")
    private List<CommitteeScoreEntryRequest> entries;
}
