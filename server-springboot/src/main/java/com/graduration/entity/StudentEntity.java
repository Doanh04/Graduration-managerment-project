package com.graduration.entity;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "student")
public class StudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_student")
    String idStudent;

    @Column(name = "student_code", nullable = false, unique = true)
    String studentCode;

    @Column(name = "full_name_student", nullable = false)
    String fullNameStudent;

    @Column(name = "avt_student")
    String pathAvt;

    @Column(name = "phone_student", length = 16, unique = true)
    String phoneStudent;

    @Column(name = "email_student", unique = true)
    String email;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    UserEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_team")
    TeamEntity team;
}
