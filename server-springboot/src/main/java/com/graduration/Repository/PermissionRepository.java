package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.graduration.Constain.PermissionConstain;
import com.graduration.entity.PermissionEntity;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, PermissionConstain> {
    boolean existsByPermissionName(String permissionName);

    boolean existsByPermissionNameAndPermissionIdNot(String permissionName, PermissionConstain permissionId);
}
