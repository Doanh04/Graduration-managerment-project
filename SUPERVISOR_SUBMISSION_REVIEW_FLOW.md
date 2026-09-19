# Luồng giảng viên hướng dẫn nhận xét tiến độ

Sơ đồ nghiệp vụ khi giảng viên hướng dẫn xem, nhận xét và duyệt hoặc không duyệt bài nộp tiến độ của sinh viên.

```mermaid
flowchart LR
    GV[Giảng viên hướng dẫn] --> FE[Chọn bài nộp tiến độ<br/>và nhập nhận xét]
    FE -->|Request duyệt/không duyệt| BE[Backend kiểm tra<br/>quyền và trạng thái bài nộp]

    BE --> Q1{Giảng viên có quyền<br/>hướng dẫn nhóm?}
    Q1 -->|Không| R1[Response lỗi:<br/>Không có quyền nhận xét bài nộp]
    R1 --> FE1[Frontend thông báo thất bại]
    FE1 --> GV

    Q1 -->|Có| Q2{Bài nộp ở trạng thái<br/>SUBMITTED hoặc IN_REVIEW?}
    Q2 -->|Không| R2[Response lỗi:<br/>Không thể xử lý bài nộp này]
    R2 --> FE2[Frontend thông báo thất bại]
    FE2 --> GV

    Q2 -->|Có| Q3{Đã nhập<br/>nhận xét?}
    Q3 -->|Không| R3[Response lỗi:<br/>Nhận xét không được để trống]
    R3 --> FE3[Frontend yêu cầu nhập nhận xét]
    FE3 --> GV

    Q3 -->|Có| Q4{Lựa chọn<br/>của giảng viên?}

    Q4 -->|Duyệt| DB1[Lưu nhận xét<br/>status = APPROVED]
    DB1 --> R4[Response thành công:<br/>Đã duyệt bài nộp]
    R4 --> FE4[Hiển thị đã duyệt cho giảng viên và sinh viên]

    Q4 -->|Yêu cầu chỉnh sửa| DB2[Lưu nhận xét<br/>status = REVISION_REQUIRED]
    DB2 --> R5[Response thành công:<br/>Yêu cầu chỉnh sửa]
    R5 --> FE5[Sinh viên xem nhận xét<br/>và nộp phiên bản mới]

    Q4 -->|Từ chối| DB3[Lưu nhận xét<br/>status = REJECTED]
    DB3 --> R6[Response thành công:<br/>Đã từ chối bài nộp]
    R6 --> FE6[Hiển thị trạng thái từ chối cho sinh viên]
```

| Lựa chọn của giảng viên | Response từ backend | Trạng thái bài nộp | Kết quả với sinh viên |
|---|---|---|---|
| Duyệt | Thành công: đã duyệt bài nộp | `APPROVED` | Bài nộp được chấp nhận, không được nộp phiên bản thay thế. |
| Yêu cầu chỉnh sửa | Thành công: yêu cầu chỉnh sửa | `REVISION_REQUIRED` | Xem nhận xét và nộp phiên bản mới. |
| Từ chối | Thành công: đã từ chối bài nộp | `REJECTED` | Xem lý do từ chối; có thể nộp lại nếu mốc vẫn cho phép nộp. |
