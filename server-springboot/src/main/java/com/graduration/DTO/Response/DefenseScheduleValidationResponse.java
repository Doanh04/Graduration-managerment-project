package com.graduration.DTO.Response;

import java.util.List;

import com.graduration.Constain.DefenseScheduleConflictTypeConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DefenseScheduleValidationResponse {
    boolean valid;
    List<Conflict> conflicts;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Conflict {
        DefenseScheduleConflictTypeConstain type;
        String message;
    }
}
