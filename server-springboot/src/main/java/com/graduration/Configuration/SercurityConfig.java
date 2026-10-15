package com.graduration.Configuration;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Arrays;

import jakarta.servlet.http.Cookie;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SercurityConfig {

    private static final Set<String> TOKEN_OPTIONAL_ENDPOINTS = Set.of(
            "/auth/cookie/login",
            "/auth/cookie/introspect",
            "/auth/cookie/refresh",
            "/auth/cookie/logout",
            "/auth/login",
            "/auth/introspect",
            "/auth/refresh",
            "/auth/logout");

    private final CustomJwtDecoder customJwtDecoder;
    private final String allowedOrigins;

    private final String[] PUBLIC_ENPOINT_POST = {
        "/auth/cookie/login",
        "/auth/cookie/introspect",
        "/auth/cookie/refresh",
        "/auth/cookie/logout",
        "/auth/introspect",
        "/auth/refresh",
        "/auth/logout",
        "/auth/login"
    };

    public SercurityConfig(
            CustomJwtDecoder customJwtDecoder,
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        this.customJwtDecoder = customJwtDecoder;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSercurity) throws Exception {
        httpSercurity.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        httpSercurity.authorizeHttpRequests(request -> request.requestMatchers(HttpMethod.POST, PUBLIC_ENPOINT_POST)
                .permitAll()
                .anyRequest()
                .authenticated());

        httpSercurity.oauth2ResourceServer(oauth2 -> oauth2.bearerTokenResolver(bearerTokenResolver())
                .jwt(jwtConfigurer -> jwtConfigurer
                        .decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));
        httpSercurity.exceptionHandling(exception -> exception.accessDeniedHandler(new JwtAccessDeniedHandler()));
        httpSercurity.csrf(AbstractHttpConfigurer::disable);

        return httpSercurity.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtauthenticationConverter = new JwtAuthenticationConverter();
        jwtauthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new LinkedHashSet<>(jwtGrantedAuthoritiesConverter.convert(jwt));
            // Faculty leadership retains its identity while inheriting every Admin operation.
            if (authorities.contains(new SimpleGrantedAuthority("ROLE_FACULTY"))) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            return authorities;
        });

        return jwtauthenticationConverter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
        return request -> {
            // Các endpoint tạo/khôi phục phiên phải hoạt động kể cả khi trình
            // duyệt còn giữ access_token cũ hoặc đã hết hạn. Nếu vẫn resolve
            // cookie tại đây, BearerTokenAuthenticationFilter sẽ trả 401 trước
            // khi controller login/refresh có cơ hội xử lý request.
            if (TOKEN_OPTIONAL_ENDPOINTS.contains(request.getServletPath())) {
                return null;
            }
            String headerToken = headerResolver.resolve(request);
            if (headerToken != null) {
                return headerToken;
            }
            if (request.getCookies() == null) {
                return null;
            }
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
            return null;
        };
    }
}
