package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.graduration.Constain.CategoryTopicConstain;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.CreateTopicRequest;
import com.graduration.DTO.Response.TopicResponse;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Service.GradurationService.TopicService;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.TopicMapper;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {
    @Mock
    TopicRepository topicRepository;

    @Mock
    DefensePeriodRepository defensePeriodRepository;

    @Mock
    TopicMapper topicMapper;

    @InjectMocks
    TopicService topicService;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "user-1", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTopic_linksDefensePeriodAndPreservesCategory() {
        CreateTopicRequest request = request();
        DefensePeriodEntity period = period(DefensePeriodConstain.ONGOING);
        TopicEntity topic = TopicEntity.builder()
                .categoryTopic(CategoryTopicConstain.STUDENT)
                .build();
        when(defensePeriodRepository.findById(10L)).thenReturn(Optional.of(period));
        when(topicMapper.toEntity(request)).thenReturn(topic);
        when(topicRepository.save(topic)).thenReturn(topic);
        when(topicMapper.toResponse(topic))
                .thenReturn(TopicResponse.builder().topicId(5L).build());

        topicService.createTopic(request);

        assertSame(period, topic.getDefensePeriod());
        assertEquals(CategoryTopicConstain.STUDENT, topic.getCategoryTopic());
        assertEquals(TopicStatusConstain.DRAFT, topic.getStatus());
        assertEquals("user-1", topic.getCreatedBy());
    }

    @Test
    void createTopic_rejectsFinishedDefensePeriod() {
        when(defensePeriodRepository.findById(10L)).thenReturn(Optional.of(period(DefensePeriodConstain.FINISHED)));

        AppException exception = assertThrows(AppException.class, () -> topicService.createTopic(request()));

        assertEquals(ErrorCode.DEFENSE_PERIOD_FINISHED, exception.getErrorCode());
        verify(topicRepository, never()).save(any());
    }

    @Test
    void submitForApproval_changesDraftStatus() {
        TopicEntity topic = topic(TopicStatusConstain.DRAFT, "user-1");
        when(topicRepository.findById(5L)).thenReturn(Optional.of(topic));
        when(topicRepository.save(topic)).thenReturn(topic);

        topicService.submitForApproval(5L);

        assertEquals(TopicStatusConstain.PENDING_APPROVAL, topic.getStatus());
    }

    @Test
    void updateOrDelete_rejectsAnotherUsersTopic() {
        TopicEntity topic = topic(TopicStatusConstain.DRAFT, "user-2");
        when(topicRepository.findById(5L)).thenReturn(Optional.of(topic));

        AppException exception = assertThrows(AppException.class, () -> topicService.deleteTopic(5L));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void approve_requiresPendingApprovalStatus() {
        TopicEntity topic = topic(TopicStatusConstain.DRAFT, "user-1");
        when(topicRepository.findById(5L)).thenReturn(Optional.of(topic));

        AppException exception = assertThrows(AppException.class, () -> topicService.approveTopic(5L));

        assertEquals(ErrorCode.TOPIC_OPERATION_NOT_ALLOWED, exception.getErrorCode());
    }

    private CreateTopicRequest request() {
        return CreateTopicRequest.builder()
                .title("Graduation topic")
                .categoryTopic(CategoryTopicConstain.STUDENT)
                .defensePeriodId(10L)
                .build();
    }

    private DefensePeriodEntity period(DefensePeriodConstain status) {
        return DefensePeriodEntity.builder().ID_Defense(10L).status(status).build();
    }

    private TopicEntity topic(TopicStatusConstain status, String createdBy) {
        return TopicEntity.builder()
                .idTopic(5L)
                .status(status)
                .createdBy(createdBy)
                .defensePeriod(period(DefensePeriodConstain.ONGOING))
                .build();
    }
}
