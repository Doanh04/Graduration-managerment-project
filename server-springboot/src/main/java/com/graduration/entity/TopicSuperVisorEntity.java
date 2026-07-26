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
@Table(name = "topic_supervisor")
//Xác định giảng viên hướng dẫn đề tài nào
public class TopicSuperVisorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_super_visor")
    Long idSuperVisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    LectureEntity lecture; //giảng viên hướng dẫn nhiều đề tài

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_topic")
    TopicEntity topic;
}
