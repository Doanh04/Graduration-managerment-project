package com.graduration.Configuration;

import java.time.LocalDateTime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.graduration.Repository.ScoreRepository;
import com.graduration.Service.ManagerService.FinalSubmissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinalSubmissionDeadlineJob {
    private final ScoreRepository scores;
    private final FinalSubmissionService service;
    private volatile boolean ready;

    @EventListener(ApplicationReadyEvent.class)
    public void ready() { ready = true; check(); }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void check() {
        if (!ready) return;
        for (Long topicId : scores.findTopicsAwaitingFinalPastDeadline(
                LocalDateTime.now(FinalSubmissionService.ZONE))) {
            try {
                service.settleDeadline(topicId);
            } catch (RuntimeException exception) {
                log.error("Could not settle final submission deadline for topic {}", topicId, exception);
            }
        }
    }
}
