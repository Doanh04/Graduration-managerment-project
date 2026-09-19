package com.graduration.DTO.Request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Dữ liệu giảng viên gửi để đề xuất đề tài kèm nhóm sinh viên thực hiện. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateSupervisorTopicProposalRequest {
    @NotBlank(message = "TOPIC_TITLE_NOT_BLANK")
    String title;

    String description;
    String objective;
    String technology;

    @NotNull(message = "DEFENSE_PERIOD_NOT_FOUND")
    Long defensePeriodId;

    @NotBlank(message = "TEAM_NAME_NOT_BLANK")
    String teamName;

    @NotEmpty(message = "STUDENT_NOT_BLANK")
    List<String> studentCodes;
}
