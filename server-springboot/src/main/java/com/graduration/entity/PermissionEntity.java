package com.graduration.entity;

import com.graduration.Constain.PermissionConstain;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "permission")
public class PermissionEntity {
    @Id
    @Column(name = "Permission_id", columnDefinition = "VARCHAR(100)")
    @Enumerated(EnumType.STRING)
    PermissionConstain permissionId;

    @Column(name = "permission_name", unique = true, columnDefinition = "VARCHAR(155)")
    String permissionName;

    @Column(name = "description")
    String description;

    @ManyToMany(mappedBy = "permission")
    Set<Roles> roles = new HashSet<>();

}
