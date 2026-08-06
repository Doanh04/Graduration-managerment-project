package com.graduration.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SercurityConfig {

    private final CustomJwtDecoder customJwtDecoder;

    private final String[] PUBLIC_ENPOINT_POST = {"/register-student/create-user", "/register-student/import", "/role/create-role"};
    private final String[] PUBLIC_ENPOINT_PUT = {"/class/{classId}"};
    private final String[] PUBLIC_ENPOINT_GET = {"/register-student/get-all-student", " /register-student/{userName}", "/register-student/{userName}"};
    private final String[] PUBLIC_ENPONT_DELETE = {"/class/{classId}"};
    private final String[] PUBLIC_ENPONT_PATCH = {"/register-student/reset-password/{userName}"};

    public SercurityConfig(CustomJwtDecoder customJwtDecoder) {
        this.customJwtDecoder = customJwtDecoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSercurity) throws Exception {
        httpSercurity.authorizeHttpRequests(request -> request.requestMatchers(HttpMethod.POST, PUBLIC_ENPOINT_POST)
                .permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_ENPOINT_GET)
                .permitAll()
                .requestMatchers(HttpMethod.DELETE, PUBLIC_ENPONT_DELETE)
                .permitAll()
                .requestMatchers(HttpMethod.PUT, PUBLIC_ENPOINT_PUT)
                .permitAll()
                .requestMatchers(HttpMethod.PATCH, PUBLIC_ENPONT_PATCH)
                .permitAll()
                .anyRequest()
                .authenticated());

        httpSercurity.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer
                        .decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));
        httpSercurity.exceptionHandling(exception -> exception.accessDeniedHandler(new JwtAccessDeniedHandler()));
        httpSercurity.csrf(AbstractHttpConfigurer::disable);

        return httpSercurity.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtauthenticationConverter = new JwtAuthenticationConverter();
        jwtauthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtauthenticationConverter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
