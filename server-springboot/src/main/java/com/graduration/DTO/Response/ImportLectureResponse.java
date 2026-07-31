package com.graduration.DTO.Response;

import java.util.ArrayList;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportLectureResponse {
    int totalRows;
    int successRows;
    int failedRows;

    @Builder.Default
    List<RegisterLectureResponse> importedLecturers = new ArrayList<>();

    @Builder.Default
    List<ImportLectureErrorResponse> errors = new ArrayList<>();
}
