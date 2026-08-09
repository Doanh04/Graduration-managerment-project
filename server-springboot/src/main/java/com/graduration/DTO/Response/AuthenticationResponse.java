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
    boolean authenticated;
    String accountType;
    Set<RoleConstain> roles;
}
