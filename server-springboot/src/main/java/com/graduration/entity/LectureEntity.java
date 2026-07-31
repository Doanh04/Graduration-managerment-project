package com.graduration.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Entity giảng viên
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

    @Column(name = "lecture_code", nullable = false, columnDefinition = "VARCHAR(100)")
    String lectureCode;

    @Column(name = "full_name_lecture", nullable = true)
    String fullNameLecture;

    @Column(name = "degree", nullable = true)
    String degree;

    @Column(name = "email_lecture", unique = true)
    String emaillecture;

    @Column(name = "phone_lecture", unique = true)
    String phoneLecture;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    UserEntity user;

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<CommentEntity> comment = new ArrayList<>();

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<TopicSuperVisorEntity> topicSuperVisor = new ArrayList<>();

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ScoreEntity> score = new ArrayList<>();

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ReviewAssignmentEntity> reviewAssignment = new ArrayList<>();

    @OneToMany(mappedBy = "lecture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ComitteesMemberEntity> comitteesMember = new ArrayList<>();
}
