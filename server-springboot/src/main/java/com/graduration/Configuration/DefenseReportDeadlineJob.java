package com.graduration.Configuration;

import java.time.LocalDateTime;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.graduration.Repository.ReviewAssignmentRepository;
import com.graduration.Service.ManagerService.DefenseReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component @RequiredArgsConstructor @Slf4j
public class DefenseReportDeadlineJob {
    private final ReviewAssignmentRepository assignments;
    private final DefenseReportService reports;
    private volatile boolean ready;
    @EventListener(ApplicationReadyEvent.class)
    public void ready() { ready = true; check(); }
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void check() {
        if (!ready) return;
        for (Long id : assignments.findDueReportAssignments(LocalDateTime.now(DefenseReportService.ZONE))) {
            try { reports.settleDeadline(id); }
            catch (RuntimeException ex) { log.error("Could not settle defense report deadline for review assignment {}", id, ex); }
        }
    }
}
