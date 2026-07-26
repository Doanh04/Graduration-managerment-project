package com.graduration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "comment")
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_comment")
    Long idComment;

    @Column(name = "Content", nullable = false)
    String content; //Nội dung lời nhận xét, góp ý hoặc sửa lỗi của giảng viên

    @Column(name = "score", nullable = false)
    String score;

    @Column(name = "create_at")
    LocalDate createAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_Submission")
    SubmistionEntity submistion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    LectureEntity lecture;
}
