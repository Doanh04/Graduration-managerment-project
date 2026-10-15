package com.graduration.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_setting")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingEntity {
    public static final int SINGLETON_ID = 1;
    public static final int DEFAULT_AUDIT_RETENTION_LIMIT = 100;

    @Id
    @Column(name = "setting_id")
    Integer settingId;

    @Column(name = "student_topic_proposal_enabled", nullable = false)
    @Builder.Default
    boolean studentTopicProposalEnabled = false;

    @Column(name = "audit_retention_limit", nullable = false)
    @Builder.Default
    int auditRetentionLimit = DEFAULT_AUDIT_RETENTION_LIMIT;

    @Column(name = "library_topics_visible", nullable = false)
    @Builder.Default
    boolean libraryTopicsVisible = true;

    @Column(name = "templates_visible", nullable = false)
    @Builder.Default
    boolean templatesVisible = true;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        if (settingId == null) {
            settingId = SINGLETON_ID;
        }
        if (auditRetentionLimit < 1) {
            auditRetentionLimit = DEFAULT_AUDIT_RETENTION_LIMIT;
        }
        updatedAt = Instant.now();
    }
}
