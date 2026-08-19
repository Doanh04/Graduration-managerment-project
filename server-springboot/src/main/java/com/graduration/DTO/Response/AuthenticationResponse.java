package com.graduration.DTO.Response;

import java.util.Set;

import com.graduration.Constain.RoleConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    String token;
    String refreshToken;
    boolean authenticated;
    String userName;
    String fullName;
    String accountType;
    Set<RoleConstain> roles;
}
