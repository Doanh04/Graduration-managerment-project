package com.graduration.DTO.Request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegistrationContactValidationTest {
    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void student_requiresEmailButAllowsMissingPhone() {
        RegisterStudentRequest request = RegisterStudentRequest.builder()
                .userName("student01")
                .password("password123")
                .studentCode("SV001")
                .fullName("Nguyen Van A")
                .email("   ")
                .phone(null)
                .build();

        Set<ConstraintViolation<RegisterStudentRequest>> violations = validator.validate(request);

        assertTrue(hasMessage(violations, "EMAIL_IS_REQUIRED"));
        assertFalse(hasMessage(violations, "INVALID_PHONE"));
    }

    @Test
    void lecturerRequiresEmailButAllowsMissingPhone() {
        RegisterLectureRequest request = RegisterLectureRequest.builder()
                .userName("lecturer01")
                .password("password123")
                .lectureCode("GV001")
                .fullName("Nguyen Van A")
                .email(null)
                .phone(null)
                .build();

        Set<ConstraintViolation<RegisterLectureRequest>> violations = validator.validate(request);

        assertTrue(hasMessage(violations, "EMAIL_IS_REQUIRED"));
        assertFalse(hasMessage(violations, "INVALID_PHONE"));
    }

    private static boolean hasMessage(Set<? extends ConstraintViolation<?>> violations, String message) {
        return violations.stream().anyMatch(violation -> message.equals(violation.getMessage()));
    }
}
