package com.graduration.entity;

import java.time.LocalDate;

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
@Table(name = "review_assignment") // phân cộng phản biện
public class ReviewAssignmentEntity {
    @Id
    @GeneratedValue
    @Column(name = "id_review")
    Long id_review;

    @Column(name = "asigned_date", nullable = false)
    LocalDate asigned_date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_topic", nullable = false)
    TopicEntity topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    LectureEntity lecture;
}
