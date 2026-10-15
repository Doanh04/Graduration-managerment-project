package com.graduration.Configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class FacultyAuthorityTest {
    private boolean grants(String scope, String authority) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "HS512")
                .claim("sub", "test-user").claim("scope", scope).build();
        return new SercurityConfig(null).jwtAuthenticationConverter().convert(jwt)
                .getAuthorities().contains(new SimpleGrantedAuthority(authority));
    }

    @Test
    void facultyInheritsAdminWithoutLosingFacultyIdentity() {
        assertTrue(grants("ROLE_FACULTY", "ROLE_ADMIN"));
        assertTrue(grants("ROLE_FACULTY", "ROLE_FACULTY"));
        assertTrue(grants("ROLE_FACULTY PERMISSION_user_read", "PERMISSION_user_read"));
    }

    @Test
    void otherRolesDoNotReceiveAdminAuthority() {
        assertFalse(grants("ROLE_STUDENT", "ROLE_ADMIN"));
        assertFalse(grants("ROLE_SUPERVISOR ROLE_REVIEWER", "ROLE_ADMIN"));
        assertFalse(grants("", "ROLE_ADMIN"));
        assertTrue(grants("ROLE_ADMIN", "ROLE_ADMIN"));
    }
}
