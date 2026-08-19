package com.graduration.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "score_detail",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_score_detail_score_criterion",
                        columnNames = {"score_id", "criterion_id"}))
public class ScoreDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "score_detail_id")
    Long scoreDetailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "score_id", nullable = false)
    ScoreEntity score;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    ScoreCriterionEntity criterion;

    @Column(name = "score_value", precision = 5, scale = 2, nullable = false)
    BigDecimal scoreValue;

    @Column(name = "weighted_score", precision = 5, scale = 2, nullable = false)
    BigDecimal weightedScore;

    @Column(name = "comment", length = 1000)
    String comment;
}
