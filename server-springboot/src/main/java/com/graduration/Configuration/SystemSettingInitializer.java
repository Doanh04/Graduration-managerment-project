package com.graduration.Configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Repository.SystemSettingRepository;
import com.graduration.entity.SystemSettingEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SystemSettingInitializer implements ApplicationRunner {
    private final SystemSettingRepository systemSettingRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!systemSettingRepository.existsById(SystemSettingEntity.SINGLETON_ID)) {
            systemSettingRepository.save(SystemSettingEntity.builder()
                    .settingId(SystemSettingEntity.SINGLETON_ID)
                    .build());
        }
    }
}
