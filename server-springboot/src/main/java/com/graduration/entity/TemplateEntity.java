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
@Table(name = "template")
public class TemplateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "template_id")
    Integer templateId;

    @Column(name = "template_name", nullable = false, unique = true)
    String templateName;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "file_path")
    String filePath;

    @Column(name = "create_at")
    LocalDate createAt;
}
