package com.graduration.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.graduration.Constain.FinalSubmissionStatusConstain;
import com.graduration.Repository.CommitteeMemberRepository;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.FinalSubmissionRepository;
import com.graduration.Repository.GraduationEnrollmentRepository;
import com.graduration.Repository.ScoreRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Service.ManagerService.FinalSubmissionService;
import com.graduration.Service.NotificationService.NotificationRecipientService;
import com.graduration.Service.NotificationService.NotificationService;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.FinalSubmissionEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;

@ExtendWith(MockitoExtension.class)
class FinalSubmissionBackupTest {
    @Mock FinalSubmissionRepository submissions;
    @Mock TopicRepository topics;
    @Mock ScoreRepository scores;
    @Mock CommitteeMemberRepository committeeMembers;
    @Mock GraduationEnrollmentRepository enrollments;
    @Mock DefensePeriodRepository defensePeriods;
    @Mock NotificationService notificationService;
    @Mock NotificationRecipientService notificationRecipientService;

    @InjectMocks FinalSubmissionService service;

    @Test
    void backupContainsReportAndLinkInformation() throws Exception {
        DefensePeriodEntity period = DefensePeriodEntity.builder()
                .ID_Defense(7L)
                .periodName("Đợt bảo vệ 2026")
                .build();
        TeamEntity team = TeamEntity.builder().idTeam(3L).nameTeam("Nhóm A").build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(11L)
                .title("Hệ thống quản lý")
                .team(team)
                .defensePeriod(period)
                .build();
        FinalSubmissionEntity submission = FinalSubmissionEntity.builder()
                .id(15L)
                .topic(topic)
                .revision(1)
                .fileName("bao-cao.pdf")
                .contentType("application/pdf")
                .fileData("PDF-CONTENT".getBytes(StandardCharsets.UTF_8))
                .sourceUrl("https://example.com/source")
                .status(FinalSubmissionStatusConstain.APPROVED)
                .submittedAt(LocalDateTime.of(2026, 10, 5, 8, 30))
                .build();

        when(defensePeriods.findById(7L)).thenReturn(java.util.Optional.of(period));
        when(submissions.findAllForBackup(7L))
                .thenReturn(List.of(submission));

        FinalSubmissionService.BackupArchive archive = service.backup(7L);

        assertThat(archive.fileName()).isEqualTo("Đợt bảo vệ 2026.zip");
        assertThat(archive.fileCount()).isEqualTo(1);
        List<String> entryNames = new ArrayList<>();
        String information = null;
        try (ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(archive.bytes()), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                if (entry.getName().endsWith("_thong-tin.txt")) {
                    information = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        assertThat(entryNames).anyMatch(name -> name.endsWith("phien-ban-1_bao-cao.pdf"));
        assertThat(information).contains("https://example.com/source");
    }
}
