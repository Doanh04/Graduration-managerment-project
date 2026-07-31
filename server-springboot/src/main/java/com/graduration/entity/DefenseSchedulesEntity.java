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
@Table(name = "defense_schedules") // lịch bảo vệ của từng đề tài.
public class DefenseSchedulesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_defense_scheduce")
    Long idDefenseScheduce;

    @Column(name = "defense_date", nullable = false) // ngày bảo vệ
    LocalDate defenseDate;

    @Column(name = "room", nullable = false)
    String room;

    @Column(name = "location", nullable = false)
    String location;

    @Column(name = "sesstion")
    String session;

    @OneToOne
    @JoinColumn(name = "id_topic", unique = true, nullable = false)
    TopicEntity topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_Comittees", nullable = false)
    DefenseCommitteesEntity defenseCommittees;
}
