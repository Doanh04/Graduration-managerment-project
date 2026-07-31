package com.graduration.Repository;

import com.graduration.Constain.PermissionConstain;
import com.graduration.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, PermissionConstain> {
    boolean existsByPermissionName(String permissionName);

    boolean existsByPermissionNameAndPermissionIdNot(
            String permissionName, PermissionConstain permissionId);
}
