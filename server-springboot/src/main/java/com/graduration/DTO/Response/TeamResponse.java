package com.graduration.DTO.Response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeamResponse {
    Long idTeam;
    String nameTeam;
    String description;
    LocalDate joinDate;
    String role;
    Long topicId;
    String topicTitle;

    @Builder.Default
    List<StudentSummary> students = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StudentSummary {
        String studentCode;
        String fullName;
        String email;
        Long classId;
        String classCode;
    }
}
