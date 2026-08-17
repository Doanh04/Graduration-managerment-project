package com.graduration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "comittees_member")
public class ComitteesMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "comittees_member_id")
    Long comitteesMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    CommitteeMemberRoleConstain role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    CommitteeMemberStatusConstain status;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    LocalDateTime assignedAt;

    @Column(name = "ended_at")
    LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    UserEntity assignedBy;

    @Column(name = "note", length = 1000)
    String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    LectureEntity lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_Comittees", nullable = false)
    DefenseCommitteesEntity defenseCommittees;

    @PrePersist
    void prePersist() {
        assignedAt = assignedAt == null ? LocalDateTime.now() : assignedAt;
        status = status == null ? CommitteeMemberStatusConstain.ACTIVE : status;
    }
}
