package com.graduration.entity;

import java.util.ArrayList;
import java.util.List;

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
@Table(name = "Submistion")
// ưu trữ thông tin mỗi khi sinh viên/nhóm sinh viên tải lên (upload) file báo cáo, sản phẩm cho từng mốc tiến độ.
public class SubmistionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID_Submission")
    Long IdSubmission;

    @Column(name = "File_Path", nullable = false) // đường dẫn lưu trữ file trên hệ thống
    String filePath;

    @Column(name = "File_Name", nullable = false) // Tên file
    String fileName;

    @Column(name = "Is_Late", nullable = false) // xacs định nộp muộn không
    Boolean isLate;

    @Column(name = "note")
    String note; // ghi chú của sinh viên về bài nộp

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Id_Miles_Stone", nullable = false)
    MilesStoneEntity milesStone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_team", nullable = false)
    TeamEntity team;

    @OneToMany(mappedBy = "submistion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<CommentEntity> comment = new ArrayList<>();
}
