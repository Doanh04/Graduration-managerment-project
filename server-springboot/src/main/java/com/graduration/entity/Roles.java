package com.graduration.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.RoleNameConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "roles")
public class Roles {
    @Id
    @Column(name = "role", columnDefinition = "VARCHAR(100)")
    @Enumerated(EnumType.STRING)
    RoleConstain role;

    @Column(name = "role_name", unique = true, columnDefinition = "VARCHAR(100)")
    @Enumerated(EnumType.STRING)
    RoleNameConstain roleName;

    @Column(name = "Description")
    String description;

    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    Set<UserEntity> user = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role"),
            inverseJoinColumns = @JoinColumn(name = "Permission_id"))
    @Builder.Default
    Set<PermissionEntity> permission = new HashSet<>();
}
