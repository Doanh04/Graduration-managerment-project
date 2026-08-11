package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.entity.LibraryTopicEntity;

public interface LibraryTopicRepository extends JpaRepository<LibraryTopicEntity, Long> {
    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdLibraryTopicNot(String title, Long idLibraryTopic);
}
