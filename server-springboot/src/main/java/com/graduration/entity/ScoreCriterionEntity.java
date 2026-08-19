package com.graduration.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.graduration.Constain.ScoreTypeConstain;

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
        name = "score_criterion",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_score_criterion_period_code_type",
                        columnNames = {"id_defense", "criterion_code", "score_type"}))
public class ScoreCriterionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "criterion_id")
    Long criterionId;

    @Column(name = "criterion_code", nullable = false)
    String criterionCode;

    @Column(name = "criterion_name", nullable = false)
    String criterionName;

    @Column(name = "description")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_type", nullable = false)
    ScoreTypeConstain scoreType;

    @Column(name = "max_score", precision = 5, scale = 2, nullable = false)
    BigDecimal maxScore;

    @Column(name = "weight", precision = 5, scale = 2, nullable = false)
    BigDecimal weight;

    @Column(name = "display_order")
    Integer displayOrder;

    @Column(name = "active", nullable = false)
    @Builder.Default
    Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_defense", nullable = false)
    DefensePeriodEntity defensePeriod;

    @OneToMany(mappedBy = "criterion")
    @Builder.Default
    List<ScoreEntity> scores = new ArrayList<>();

    @OneToMany(mappedBy = "criterion")
    @Builder.Default
    List<ScoreDetailEntity> scoreDetails = new ArrayList<>();
}
