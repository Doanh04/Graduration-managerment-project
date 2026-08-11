package com.graduration.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
@Table(name = "team")
public class TeamEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_team")
    Long idTeam;

    @Column(name = "name_team")
    String nameTeam;

    @Column(name = "description")
    String description;

    @Column(name = "join_date")
    LocalDate joinDate;

    @Column(name = "role")
    String role;

    @OneToMany(mappedBy = "team")
    @Builder.Default
    List<StudentEntity> studentEntities = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "id_topic", referencedColumnName = "id_topic")
    TopicEntity topic;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<SubmistionEntity> submistion = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ScoreEntity> score = new ArrayList<>();
}
