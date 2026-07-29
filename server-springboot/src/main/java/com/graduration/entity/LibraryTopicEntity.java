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

    @Column(name = "description")
    String description;

    @Column(name = "objective")
    String objective;

    @Column(name = "technology")
    String technology;
}
