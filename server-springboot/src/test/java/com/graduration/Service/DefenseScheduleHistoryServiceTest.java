package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.graduration.Constain.DefenseScheduleHistoryActionConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.Repository.DefenseScheduleHistoryRepository;
import com.graduration.Repository.DefenseScheduleRepository;
import com.graduration.Service.ManagerService.DefenseScheduleHistoryService;
import com.graduration.Service.ManagerService.DefenseScheduleHistoryService.Snapshot;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.DefenseScheduleHistoryEntity;
import com.graduration.entity.DefenseSchedulesEntity;
import com.graduration.entity.UserEntity;

@ExtendWith(MockitoExtension.class)
class DefenseScheduleHistoryServiceTest {
    @Mock
    DefenseScheduleHistoryRepository historyRepository;

    @Mock
    DefenseScheduleRepository scheduleRepository;

    @InjectMocks
    DefenseScheduleHistoryService historyService;

    @Test
    void recordCreated_storesOnlyNewSnapshot() {
        DefenseSchedulesEntity schedule =
                DefenseSchedulesEntity.builder().idDefenseScheduce(10L).build();
        UserEntity actor = UserEntity.builder().userId("admin").build();
        Snapshot after = snapshot(DefenseScheduleStatusConstain.DRAFT, "A101");

        historyService.record(schedule, DefenseScheduleHistoryActionConstain.CREATED, null, after, null, actor);

        ArgumentCaptor<DefenseScheduleHistoryEntity> captor =
                ArgumentCaptor.forClass(DefenseScheduleHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertEquals(
                DefenseScheduleHistoryActionConstain.CREATED, captor.getValue().getAction());
        assertNull(captor.getValue().getPreviousStatus());
        assertEquals(DefenseScheduleStatusConstain.DRAFT, captor.getValue().getNewStatus());
        assertEquals("A101", captor.getValue().getNewRoom());
    }

    @Test
    void recordRescheduled_preservesOldAndNewValuesAndReason() {
        DefenseSchedulesEntity schedule =
                DefenseSchedulesEntity.builder().idDefenseScheduce(10L).build();
        Snapshot before = snapshot(DefenseScheduleStatusConstain.POSTPONED, "A101");
        Snapshot after = snapshot(DefenseScheduleStatusConstain.DRAFT, "A102");

        historyService.record(
                schedule,
                DefenseScheduleHistoryActionConstain.RESCHEDULED,
                before,
                after,
                " Changed room ",
                UserEntity.builder().userId("admin").build());

        ArgumentCaptor<DefenseScheduleHistoryEntity> captor =
                ArgumentCaptor.forClass(DefenseScheduleHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertEquals("A101", captor.getValue().getOldRoom());
        assertEquals("A102", captor.getValue().getNewRoom());
        assertEquals("Changed room", captor.getValue().getReason());
    }

    @Test
    void snapshot_copiesCommitteeIdentityInsteadOfKeepingOnlyRelation() {
        DefenseCommitteesEntity committee = DefenseCommitteesEntity.builder()
                .idComittees(3L)
                .comitteesName("Council 1")
                .build();
        DefenseSchedulesEntity schedule = DefenseSchedulesEntity.builder()
                .defenseCommittees(committee)
                .defenseDate(LocalDate.of(2026, 12, 10))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(8, 45))
                .room("A101")
                .status(DefenseScheduleStatusConstain.PUBLISHED)
                .build();

        Snapshot result = historyService.snapshot(schedule);

        assertEquals(3L, result.committeeId());
        assertEquals("Council 1", result.committeeName());
        assertEquals(DefenseScheduleStatusConstain.PUBLISHED, result.status());
    }

    private Snapshot snapshot(DefenseScheduleStatusConstain status, String room) {
        return new Snapshot(
                LocalDate.of(2026, 12, 10),
                LocalTime.of(8, 0),
                LocalTime.of(8, 45),
                room,
                "Building A",
                3L,
                "Council 1",
                status);
    }
}
