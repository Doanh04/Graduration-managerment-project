package com.graduration.exception;

import java.util.Map;
import java.util.Objects;

import jakarta.validation.ConstraintViolation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.graduration.DTO.Response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    // Hàm xử lý lỗi chủ động
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<?> apiResponse = new ApiResponse<>();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMesage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    //    exception lỗi password
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<?>> handlingValidation(MethodArgumentNotValidException exception) {
        var validationError = exception.getBindingResult().getAllErrors().get(0);
        String enumKey = validationError.getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;
        try {
            if (enumKey != null) {
                errorCode = ErrorCode.valueOf(enumKey);
            }

            var constraintViolation = validationError.unwrap(ConstraintViolation.class);

            attributes = constraintViolation.getConstraintDescriptor().getAttributes();

            log.info(attributes.toString());

        } catch (RuntimeException ignored) {
            log.debug("Validation message does not map to an ErrorCode: {}", enumKey);
        }

        ApiResponse<?> apiResponse = new ApiResponse<>();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(
                Objects.nonNull(attributes) ? mapAttribute(errorCode.getMesage(), attributes) : errorCode.getMesage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse<?>> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMesage())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingUncategorizedException(Exception exception) {
        log.error("Uncategorized exception", exception);

        ErrorCode errorCode = ErrorCode.UKNOWN_ERROR;
        ApiResponse<?> apiResponse = new ApiResponse<>();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMesage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    private String mapAttribute(String message, Map<String, Object> attributes) {
        // Duyệt qua từng Key-Value có trong túi thuộc tính của Annotation
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey(); // Ví dụ: "min", "max", "value", "regexp"
            String value = String.valueOf(entry.getValue()); // Ví dụ: 6, 20, 18

            // Cứ thấy chỗ nào trong câu message có dạng {key} thì thay bằng value
            message = message.replace("{" + key + "}", value);
        }
        return message;
    }
}
