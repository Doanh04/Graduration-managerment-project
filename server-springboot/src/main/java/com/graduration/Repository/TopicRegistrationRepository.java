package com.graduration.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.Constain.TopicRegistrationStatusConstain;
import com.graduration.entity.TopicRegistrationEntity;

public interface TopicRegistrationRepository extends JpaRepository<TopicRegistrationEntity, Long> {
    Page<TopicRegistrationEntity> findByStatusOrderBySubmittedAtDesc(
            TopicRegistrationStatusConstain status, Pageable pageable);

    Page<TopicRegistrationEntity> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    List<TopicRegistrationEntity> findByEnrollment_Student_UserEntity_UserIdOrderBySubmittedAtDesc(String userId);

    boolean existsByEnrollment_EnrollmentIdAndStatus(Long enrollmentId, TopicRegistrationStatusConstain status);

    boolean existsByEnrollment_EnrollmentIdAndTopic_IdTopicAndStatus(
            Long enrollmentId, Long topicId, TopicRegistrationStatusConstain status);

    List<TopicRegistrationEntity> findByTeam_IdTeamAndStatus(Long teamId, TopicRegistrationStatusConstain status);

    List<TopicRegistrationEntity> findByTeam_IdTeamOrderBySubmittedAtDesc(Long teamId);

    Optional<TopicRegistrationEntity> findByEnrollment_EnrollmentIdAndPriority(Long enrollmentId, Integer priority);
}
