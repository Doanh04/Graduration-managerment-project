# Luồng sinh viên nộp tiến độ

Sơ đồ ngắn gọn cho nghiệp vụ nộp tiến độ, tập trung vào thời hạn nộp.

```mermaid
flowchart LR
    SV[Sinh viên] --> FE[Tạo bài nộp<br/>chọn tệp và mốc tiến độ]
    FE -->|Request nộp bài| BE[Backend kiểm tra<br/>mốc và thời hạn]

    BE --> Q1{Mốc tiến độ<br/>đang mở?}
    Q1 -->|Không| R1[Response lỗi:<br/>Mốc tiến độ chưa mở hoặc đã đóng]
    R1 --> FE1[Frontend thông báo thất bại]
    FE1 --> SV

    Q1 -->|Có| Q2{Thời điểm nộp<br/>có quá deadline?}
    Q2 -->|Không| DB1[Lưu bài nộp<br/>is_late = false]
    DB1 --> R2[Response thành công:<br/>Nộp bài đúng hạn]
    R2 --> FE2[Frontend thông báo thành công]
    FE2 --> SV

    Q2 -->|Có| Q3{Mốc có cho phép<br/>nộp muộn?}
    Q3 -->|Không| R3[Response lỗi:<br/>Đã quá hạn nộp]
    R3 --> FE3[Frontend thông báo<br/>Không thể nộp bài]
    FE3 --> SV

    Q3 -->|Có| DB2[Lưu bài nộp<br/>is_late = true]
    DB2 --> R4[Response thành công:<br/>Nộp bài muộn]
    R4 --> FE4[Frontend thông báo thành công<br/>và hiển thị nhãn Nộp muộn]
    FE4 --> SV
```

| Trường hợp | Response từ backend | Dữ liệu lưu trong DB |
|---|---|---|
| Mốc chưa mở/đã đóng | Lỗi: không được nộp bài | Không lưu dữ liệu |
| Nộp trước hoặc đúng deadline | Thành công: nộp đúng hạn | `is_late = false`, `status = SUBMITTED` |
| Nộp sau deadline, không cho phép muộn | Lỗi: đã quá hạn nộp | Không lưu dữ liệu |
| Nộp sau deadline, cho phép muộn | Thành công: nộp muộn | `is_late = true`, `status = SUBMITTED` |
