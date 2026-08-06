package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.RoleNameConstain;
import com.graduration.entity.Roles;

@Repository
public interface RoleRepository extends JpaRepository<Roles, RoleConstain> {
    boolean existsByRoleName(RoleNameConstain roleName);

    boolean existsByRoleNameAndRoleNot(RoleNameConstain roleName, RoleConstain role);
}
