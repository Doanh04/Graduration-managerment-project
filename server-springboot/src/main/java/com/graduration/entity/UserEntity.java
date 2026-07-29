package com.graduration.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

import com.graduration.Constain.StatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "user")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    String userId;

    @Column(name = "user_name", columnDefinition = "VARCHAR(255)", unique = true, nullable = false)
    String userName;

    @Column(name = "password", columnDefinition = "VARCHAR(255)", nullable = false)
    String password;

    @Column(name = "email", columnDefinition = "VARCHAR(255)", unique = true, nullable = false)
    String email;

    @Column(name = "phone", columnDefinition = "VARCHAR(12)", unique = true)
    String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "VARCHAR(100)")
    StatusConstain status;

    @Column(name = "create_at")
    LocalDateTime createAt;

    @Column(name = "avt")
    String avt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role"))
    @Builder.Default
    Set<Roles> roles = new HashSet<>();

    @OneToOne(mappedBy = "userEntity", cascade = CascadeType.ALL)
    StudentEntity student;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    LectureEntity lecture;
}
