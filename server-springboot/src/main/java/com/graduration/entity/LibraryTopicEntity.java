package com.graduration.entity;

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
@Table(name = "library_topic")
public class LibraryTopicEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_library_topic")
    Long idLibraryTopic;

    @Column(name = "title")
    String title;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Lob
    @Column(name = "objective", columnDefinition = "TEXT")
    String objective;

    @Lob
    @Column(name = "technology", columnDefinition = "TEXT")
    String technology;
}
