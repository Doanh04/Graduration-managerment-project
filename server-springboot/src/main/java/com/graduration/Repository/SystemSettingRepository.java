package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.entity.SystemSettingEntity;

public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, Integer> {}
