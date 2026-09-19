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

    /**
     * Thành viên được lưu theo nhóm, thay vì chỉ bằng khóa ngoại hiện thời trên
     * student. Nhờ đó một sinh viên có thể có nhóm khác ở các đợt bảo vệ khác
     * mà vẫn giữ nguyên lịch sử nhóm cũ.
     */
    @ManyToMany
    @JoinTable(
            name = "team_student",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"))
    @Builder.Default
    List<StudentEntity> studentEntities = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defense_period_id")
    DefensePeriodEntity defensePeriod;

    @OneToOne
    @JoinColumn(name = "id_topic", referencedColumnName = "id_topic")
    TopicEntity topic;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<SubmistionEntity> submistion = new ArrayList<>();

    @OneToMany(mappedBy = "team")
    @Builder.Default
    List<TopicRegistrationEntity> topicRegistrations = new ArrayList<>();
}
