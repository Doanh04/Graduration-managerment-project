package com.graduration.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.graduration.Constain.CategoryTopicConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "topic")
public class TopicEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_topic")
    Long idTopic;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "description")
    String description;

    @Column(name = "objective")
    String objective; // mục tiêu của đề tài

    @Column(name = "technology")
    String technology;

    @Column(name = "category_topic")
    @Enumerated(EnumType.STRING)
    CategoryTopicConstain categoryTopic;

    @OneToOne(mappedBy = "topic", cascade = CascadeType.ALL)
    TeamEntity team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_Defense", nullable = false)
    DefensePeriodEntity defensePeriod;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<TopicSuperVisorEntity> topicSuperVisorEntities = new ArrayList<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ReviewAssignmentEntity> reviewAssignment = new ArrayList<>();

    @OneToOne(mappedBy = "topic", cascade = CascadeType.ALL)
    DefenseSchedulesEntity defenseSchedule;
}
