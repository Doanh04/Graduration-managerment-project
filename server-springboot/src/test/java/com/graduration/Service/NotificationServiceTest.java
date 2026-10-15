package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.graduration.Constain.NotificationTypeConstain;
import com.graduration.Repository.NotificationRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.Service.NotificationService.NotificationEvent;
import com.graduration.Service.NotificationService.NotificationService;
import com.graduration.entity.NotificationDocument;

class NotificationServiceTest {
    @Test
    void publishFailureDoesNotAffectBusinessFlow() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        doThrow(new IllegalStateException("notification unavailable")).when(publisher).publishEvent(any(Object.class));
        NotificationService service = new NotificationService(notifications, users, publisher);

        assertDoesNotThrow(() -> service.notifyUsers(
                Set.of("user-1"),
                "Title",
                "Content",
                NotificationTypeConstain.SYSTEM,
                "TOPIC",
                1L));
    }

    @Test
    void mongoFailureAfterCommitDoesNotAffectCompletedBusinessFlow() {
        NotificationRepository notifications = mock(NotificationRepository.class);
        UserRepository users = mock(UserRepository.class);
        doThrow(new IllegalStateException("mongo unavailable"))
                .when(notifications)
                .save(any(NotificationDocument.class));
        NotificationService service = new NotificationService(notifications, users, mock(ApplicationEventPublisher.class));
        NotificationEvent event = new NotificationEvent(
                Set.of("user-1"),
                Set.of(),
                "Title",
                "Content",
                NotificationTypeConstain.SYSTEM,
                "TOPIC",
                "1",
                false);

        assertDoesNotThrow(() -> service.deliverAfterCommit(event));
    }
}
