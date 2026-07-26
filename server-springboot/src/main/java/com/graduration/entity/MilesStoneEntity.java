package com.graduration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
//Các mốc tiến độ / Giai đoạn
@Table(name = "Miles_Stone")
public class MilesStoneEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "Id_Miles_Stone")
    Long IdMilesStone;

    @Column(name = "Milestone_Name", nullable = false) //Tên mốc tiến độ
    String milesStoneName;

    @Column(name = "description") //Mô tả chi tiết yêu cầu của mốc đó
    String Description;

    @Column(name = "Deadline", nullable = false)
    LocalDateTime deadLine; //Hạn chót (ngày giờ) phải hoàn thành.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Id_Defense")
    DefensePeriodEntity defensePeriod;

    @OneToMany(mappedBy = "milesStone", cascade = CascadeType.ALL, orphanRemoval = true)
    List<SubmistionEntity> submistion = new ArrayList<>();

}
