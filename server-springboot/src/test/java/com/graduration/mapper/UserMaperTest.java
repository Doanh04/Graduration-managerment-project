package com.graduration.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.graduration.DTO.Request.UpdateLecturerRequest;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.UserEntity;

class UserMaperTest {
    UserMaper userMaper = new UserMaperImpl();

    @Test
    void patchMapping_updatesProvidedFieldsAndPreservesNullFields() {
        UserEntity user = UserEntity.builder()
                .userName("oldUsername")
                .password("existing-password")
                .build();
        LectureEntity lecturer = LectureEntity.builder()
                .lectureCode("GV001")
                .fullNameLecture("Old Name")
                .emaillecture("old@example.com")
                .phoneLecture("0901234567")
                .build();
        UpdateLecturerRequest request = UpdateLecturerRequest.builder()
                .fullName("Updated Name")
                .build();

        userMaper.updateUserEntity(request, user);
        userMaper.updateLecturerEntity(request, lecturer);

        assertEquals("oldUsername", user.getUserName());
        assertEquals("existing-password", user.getPassword());
        assertEquals("GV001", lecturer.getLectureCode());
        assertEquals("Updated Name", lecturer.getFullNameLecture());
        assertEquals("old@example.com", lecturer.getEmaillecture());
        assertEquals("0901234567", lecturer.getPhoneLecture());
    }
}
