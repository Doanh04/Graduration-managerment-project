package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.entity.TopicEntity;

public interface TopicRepository extends JpaRepository<TopicEntity, Long> {}
