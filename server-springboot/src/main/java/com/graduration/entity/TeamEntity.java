package com.graduration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    List<StudentEntity> studentEntities = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "id_topic", referencedColumnName = "id_topic")
    TopicEntity topic;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    List<SubmistionEntity> submistion = new ArrayList<>();
}
