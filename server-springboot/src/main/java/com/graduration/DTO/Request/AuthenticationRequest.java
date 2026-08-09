package com.graduration.DTO.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {
    /**
     * Common login field. It may contain a username, lecturer code or student code.
     */
    String identifier;

    /** Kept for backward compatibility with the existing login payload. */
    String userName;

    String lecturerCode;
    String studentCode;
    String password;
}
