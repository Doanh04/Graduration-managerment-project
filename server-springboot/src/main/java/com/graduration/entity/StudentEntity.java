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

    @Column(name = "student_code", unique = true)
    String studentCode;

    @Column(name = "full_name_student", nullable = false)
    String fullNameSStudent;

    @Column(name = "avt_student")
    String pathAvt;

    @Column(name = "phone_student", columnDefinition = "VARCHAR(12)")
    String phoneStudent;

    @Column(name = "email_student")
    String email;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    UserEntity userEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    ClassEntity classEntity;
}
