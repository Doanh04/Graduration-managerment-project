package com.graduration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.SupervisorRoleConstain;

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
// Xác định giảng viên hướng dẫn đề tài nào
public class TopicSuperVisorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_super_visor")
    Long idSuperVisor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecture_id", nullable = false)
    LectureEntity lecture; // giảng viên hướng dẫn nhiều đề tài

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_topic", nullable = false)
    TopicEntity topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "supervisor_role", nullable = false)
    SupervisorRoleConstain supervisorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    SupervisorAssignmentStatusConstain status;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    LocalDateTime assignedAt;

    @Column(name = "ended_at")
    LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    UserEntity assignedBy;

    @Column(name = "note", length = 1000)
    String note;

    @PrePersist
    void prePersist() {
        assignedAt = assignedAt == null ? LocalDateTime.now() : assignedAt;
        supervisorRole = supervisorRole == null ? SupervisorRoleConstain.PRIMARY : supervisorRole;
        status = status == null ? SupervisorAssignmentStatusConstain.ACTIVE : status;
    }
}
