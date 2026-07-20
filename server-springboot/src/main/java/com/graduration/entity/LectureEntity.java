package com.graduration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

//Entity giảng viên
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "lecture")
public class LectureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "lecture_id")
    String lectureId;

    @Column(name = "lecture_code", nullable = false ,columnDefinition = "VARCHAR(100)")
    String lectureCode;

    @Column(name = "full_name_lecture", nullable = true)
    String fullNameLecture;

    @Column(name = "degree", nullable = true)
    String degree;

    @Column(name = "email_lecture", unique = true, nullable = false)
    String email_lecture;

    @Column(name = "phone_lecture", unique = true, nullable = false)
    String phoneLecture;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    UserEntity user;
}
