package com.graduration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

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

    @Column(name = "description")
    String description;

    @Column(name = "file_path")
    String filePath;

    @Column(name = "create_at")
    LocalDate createAt;
}
