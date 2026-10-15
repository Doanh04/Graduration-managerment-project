package com.graduration.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

/** One immutable upload version; review decisions belong to this exact version. */
@Entity
@Table(name = "defense_report", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"schedule_id", "revision"}),
        @UniqueConstraint(columnNames = {"review_assignment_id", "revision"})
})
@Getter @Setter @NoArgsConstructor
public class DefenseReportEntity {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    /** Legacy link kept nullable so uploads created by the first schedule-based version remain readable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private DefenseSchedulesEntity schedule;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_assignment_id")
    private ReviewAssignmentEntity reviewAssignment;
    private int revision;
    private String fileName;
    private String contentType;
    @Lob @Column(columnDefinition = "LONGBLOB", nullable = false)
    private byte[] fileData;
    @Column(length = 2000)
    private String sourceUrl;
    @Column(length = 2000)
    private String productUrl;
    @Column(length = 4000)
    private String note;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private String status; // PENDING, REVISION_REQUIRED, APPROVED, REJECTED
    @Column(length = 4000)
    private String reviewComment;
    private String reviewedBy;
    private String reviewerName;
    private LocalDateTime reviewedAt;
}
