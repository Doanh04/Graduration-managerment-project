package com.graduration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.graduration.Constain.FinalSubmissionStatusConstain;

import lombok.*;

@Entity
@Table(
        name = "final_submission",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_final_submission_topic_revision",
                columnNames = {"id_topic", "revision"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalSubmissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_topic", nullable = false)
    private TopicEntity topic;

    @Column(nullable = false)
    private int revision;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Lob
    @Column(columnDefinition = "LONGBLOB", nullable = false)
    private byte[] fileData;

    @Column(length = 2000)
    private String sourceUrl;

    @Column(length = 2000)
    private String productUrl;

    @Column(length = 4000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FinalSubmissionStatusConstain status;

    private String submittedBy;
    private LocalDateTime submittedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;

    @Column(length = 4000)
    private String reviewComment;
}
