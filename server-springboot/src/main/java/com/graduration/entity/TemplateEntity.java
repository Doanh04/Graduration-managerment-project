package com.graduration.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.graduration.Constain.TemplateStatusConstain;
import com.graduration.Constain.TemplateTypeConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "template",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_template_name_version_period",
                        columnNames = {"template_name", "version", "id_defense"}))
public class TemplateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "template_id")
    Integer templateId;

    @Column(name = "template_name", nullable = false)
    String templateName;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "file_path")
    String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type")
    TemplateTypeConstain templateType;

    @Column(name = "version")
    Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    TemplateStatusConstain status;

    @Column(name = "original_file_name")
    String originalFileName;

    @Column(name = "content_type")
    String contentType;

    @Column(name = "file_size")
    Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    UserEntity uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_defense")
    DefensePeriodEntity defensePeriod;

    @Column(name = "create_at")
    LocalDate createAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createAt = createAt == null ? now.toLocalDate() : createAt;
        updatedAt = now;
        version = version == null ? 1 : version;
        status = status == null ? TemplateStatusConstain.ACTIVE : status;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
