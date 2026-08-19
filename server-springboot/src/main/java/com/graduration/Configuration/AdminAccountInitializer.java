package com.graduration.Configuration;

import java.time.LocalDateTime;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.RoleNameConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.Repository.RoleRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.Roles;
import com.graduration.entity.UserEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap-admin.username:admin.bootstrap}")
    private String userName;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || password == null || password.isBlank() || userRepository.existsByUserName(userName)) return;

        Roles adminRole = roleRepository
                .findById(RoleConstain.ADMIN)
                .orElseGet(() -> roleRepository.save(Roles.builder()
                        .role(RoleConstain.ADMIN)
                        .roleName(RoleNameConstain.NAME_ADMIN)
                        .description("System administrator")
                        .build()));

        var roles = new HashSet<Roles>();
        roles.add(adminRole);
        userRepository.save(UserEntity.builder()
                .userName(userName)
                .password(passwordEncoder.encode(password))
                .status(StatusConstain.ACTIVE)
                .createAt(LocalDateTime.now())
                .roles(roles)
                .build());
        log.warn("Bootstrap administrator account '{}' was created; change its password after signing in", userName);
    }
}
