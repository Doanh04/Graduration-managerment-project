package com.graduration.Configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Constain.MilesStoneStatusConstain;
import com.graduration.Constain.NotificationTypeConstain;
import com.graduration.Repository.MilestoneRepository;
import com.graduration.Service.NotificationService.NotificationRecipientService;
import com.graduration.Service.NotificationService.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MilestoneNotificationJob {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MilestoneRepository milestoneRepository;
    private final NotificationService notificationService;
    private final NotificationRecipientService notificationRecipientService;

    @Scheduled(cron = "0 */15 * * * *", zone = "${app.time-zone:Asia/Ho_Chi_Minh}")
    @Transactional(readOnly = true)
    public void notifyDeadlines() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime approachingLimit = now.plusHours(24);
        milestoneRepository
                .findDeadlinesBetween(MilesStoneStatusConstain.OPEN, now, approachingLimit)
                .forEach(milestone -> notificationService.notifyUsersOnce(
                        notificationRecipientService.studentUserIdsForDefensePeriod(
                                milestone.getDefensePeriod().getID_Defense()),
                        "Tiến độ sắp hết hạn",
                        "Tiến độ \"" + milestone.getMilesStoneName() + "\" sẽ hết hạn lúc "
                                + milestone.getDeadLine().format(DISPLAY_TIME) + ".",
                        NotificationTypeConstain.DEADLINE_APPROACHING,
                        "MILESTONE",
                        milestone.getIdMilesStone()));
        milestoneRepository.findExpired(MilesStoneStatusConstain.OPEN, now)
                .forEach(milestone -> notificationService.notifyUsersOnce(
                        notificationRecipientService.studentUserIdsForDefensePeriod(
                                milestone.getDefensePeriod().getID_Defense()),
                        "Tiến độ đã hết hạn",
                        "Tiến độ \"" + milestone.getMilesStoneName() + "\" đã hết hạn lúc "
                                + milestone.getDeadLine().format(DISPLAY_TIME) + ".",
                        NotificationTypeConstain.DEADLINE_EXPIRED,
                        "MILESTONE",
                        milestone.getIdMilesStone()));
    }
}
