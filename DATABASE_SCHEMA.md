# Tài liệu cấu trúc database

Tài liệu này mô tả database MySQL `graduration-managerment` của hệ thống quản lý đồ án tốt nghiệp. Quy ước: `PK` là khóa chính, `FK` là khóa ngoại, `UNIQUE` là giá trị không trùng.

> Lưu ý: sau lần khởi động backend tiếp theo, bảng `submistion` có thêm cột `file_data` (`LONGBLOB`) để lưu nội dung file nộp trực tiếp trong MySQL.

## 1. Tổ chức, người dùng và phân quyền

### `user`

Lưu tài khoản đăng nhập chung cho Admin, giảng viên và sinh viên.

| Trường | Ý nghĩa |
|---|---|
| `user_id` (PK) | Mã định danh tài khoản. |
| `avt` | Đường dẫn/giá trị ảnh đại diện. |
| `create_at` | Thời điểm tạo tài khoản. |
| `password` | Mật khẩu đã mã hóa. |
| `status` | Trạng thái hoạt động của tài khoản. |
| `user_name` (UNIQUE) | Tên đăng nhập. |

### `roles`

Danh mục vai trò hệ thống, ví dụ `ADMIN`, `STUDENT`, `LECTURER`.

| Trường | Ý nghĩa |
|---|---|
| `role` (PK) | Mã role được dùng trong phân quyền. |
| `description` | Mô tả vai trò. |
| `role_name` (UNIQUE) | Tên hiển thị role. |

### `permission`

Danh mục quyền thao tác chi tiết.

| Trường | Ý nghĩa |
|---|---|
| `permission_id` (PK) | Mã permission. |
| `description` | Mô tả quyền. |
| `permission_name` (UNIQUE) | Tên quyền hiển thị. |

### `user_role`

Bảng liên kết nhiều-nhiều giữa tài khoản và role.

| Trường | Ý nghĩa |
|---|---|
| `user_id` (PK, FK → `user.user_id`) | Tài khoản được gán role. |
| `role` (PK, FK → `roles.role`) | Role được gán. |

### `role_permission`

Bảng liên kết nhiều-nhiều giữa role và permission.

| Trường | Ý nghĩa |
|---|---|
| `role` (PK, FK → `roles.role`) | Role sở hữu quyền. |
| `permission_id` (PK, FK → `permission.permission_id`) | Permission được cấp. |

### `invalidated`

Lưu token đã bị thu hồi để ngăn token cũ tiếp tục sử dụng.

| Trường | Ý nghĩa |
|---|---|
| `id` (PK) | Định danh/JTI của token bị vô hiệu hóa. |
| `expiry_time` | Thời điểm token hết hạn để có thể dọn dẹp. |

### `lecture`

Hồ sơ giảng viên, liên kết tùy chọn với tài khoản đăng nhập.

| Trường | Ý nghĩa |
|---|---|
| `lecture_id` (PK) | Mã định danh hồ sơ giảng viên. |
| `degree` | Học vị/học hàm. |
| `email_lecture` (UNIQUE) | Email giảng viên. |
| `full_name_lecture` | Họ tên giảng viên. |
| `lecture_code` (UNIQUE) | Mã giảng viên. |
| `phone_lecture` (UNIQUE) | Số điện thoại. |
| `user_id` (UNIQUE, FK → `user.user_id`) | Tài khoản của giảng viên. |

### `student`

Hồ sơ sinh viên. Liên kết nhóm dùng `team_student` để một sinh viên có thể có nhóm ở nhiều đợt bảo vệ.

| Trường | Ý nghĩa |
|---|---|
| `id_student` (PK) | Mã định danh hồ sơ sinh viên. |
| `email_student` (UNIQUE) | Email sinh viên. |
| `full_name_student` | Họ tên sinh viên. |
| `avt_student` | Ảnh đại diện. |
| `phone_student` (UNIQUE) | Số điện thoại. |
| `student_code` (UNIQUE) | Mã số sinh viên. |
| `class_id` (FK → `class.class_id`) | Lớp hành chính. |
| `user_id` (UNIQUE, FK → `user.user_id`) | Tài khoản đăng nhập của sinh viên. |

## 2. Danh mục đào tạo và đợt bảo vệ

### `major`

Danh mục ngành/chuyên ngành.

| Trường | Ý nghĩa |
|---|---|
| `major_id` (PK) | Mã ngành. |
| `description` | Mô tả ngành. |
| `major_name` (UNIQUE) | Tên ngành. |
| `major_code` (UNIQUE) | Mã ngành. |

### `class`

Danh mục lớp sinh viên.

| Trường | Ý nghĩa |
|---|---|
| `class_id` (PK) | Mã lớp. |
| `class_code` (UNIQUE) | Mã lớp. |
| `class_name` (UNIQUE) | Tên lớp. |
| `description` | Ghi chú lớp. |
| `major_id` (FK → `major.major_id`) | Ngành quản lý lớp. |

### `academic_year`

Danh mục năm học.

| Trường | Ý nghĩa |
|---|---|
| `academic_id` (PK) | Mã năm học. |
| `academic_year` (UNIQUE) | Nhãn năm học, ví dụ `2026-2027`. |
| `description` | Mô tả/ghi chú. |

### `defense_period`

Đợt triển khai và bảo vệ đồ án trong một năm học.

| Trường | Ý nghĩa |
|---|---|
| `id_defense` (PK) | Mã đợt bảo vệ. |
| `end_date` | Ngày kết thúc đợt. |
| `period_name` | Tên đợt. |
| `project_type` | Loại đồ án áp dụng cho đợt. |
| `start_date` | Ngày bắt đầu đợt. |
| `status` | Trạng thái đợt bảo vệ. |
| `academic_id` (FK → `academic_year.academic_id`) | Năm học chứa đợt. |

### `graduation_enrollment`

Ghi danh sinh viên vào đợt bảo vệ; là điều kiện để sinh viên thực hiện đồ án trong đợt đó.

| Trường | Ý nghĩa |
|---|---|
| `enrollment_id` (PK) | Mã hồ sơ ghi danh. |
| `completed_at` | Thời điểm hoàn tất/rút hồ sơ. |
| `enrolled_at` | Thời điểm ghi danh. |
| `note` | Ghi chú. |
| `status` | Trạng thái: đủ điều kiện, đã ghi danh, đang thực hiện, hoàn thành, không đạt hoặc rút. |
| `id_defense` (FK → `defense_period.id_defense`) | Đợt bảo vệ. |
| `id_student` (FK → `student.id_student`) | Sinh viên ghi danh. |

## 3. Nhóm, đề tài và hướng dẫn

### `team`

Nhóm sinh viên. Mỗi nhóm được gắn với một đợt bảo vệ để dữ liệu nhóm không bị dùng chung giữa các đợt.

| Trường | Ý nghĩa |
|---|---|
| `id_team` (PK) | Mã nhóm. |
| `description` | Mô tả/ghi chú nhóm. |
| `join_date` | Ngày tạo/tham gia nhóm. |
| `name_team` | Tên nhóm. |
| `role` | Thông tin vai trò nhóm cũ/ghi chú. |
| `id_topic` (UNIQUE, FK → `topic.id_topic`) | Đề tài được gán cho nhóm. |
| `defense_period_id` (FK → `defense_period.id_defense`) | Đợt bảo vệ của nhóm. |

### `team_student`

Liên kết nhiều-nhiều giữa nhóm và sinh viên. Quy tắc nghiệp vụ đảm bảo một sinh viên chỉ thuộc một nhóm trong cùng một đợt bảo vệ.

| Trường | Ý nghĩa |
|---|---|
| `team_id` (FK → `team.id_team`) | Nhóm. |
| `student_id` (FK → `student.id_student`) | Thành viên nhóm. |

### `topic`

Đề tài đồ án do giảng viên hoặc sinh viên đề xuất.

| Trường | Ý nghĩa |
|---|---|
| `id_topic` (PK) | Mã đề tài. |
| `category_topic` | Nguồn đề tài: giảng viên hoặc sinh viên. |
| `created_at` | Thời điểm tạo. |
| `created_by` | Người tạo (metadata). |
| `description` | Mô tả đề tài. |
| `objective` | Mục tiêu cần đạt. |
| `rejection_reason` | Lý do từ chối nếu có. |
| `status` | Trạng thái vòng đời đề tài. |
| `technology` | Công nghệ dự kiến sử dụng. |
| `title` | Tên đề tài. |
| `updated_at` | Thời điểm cập nhật gần nhất. |
| `id_defense` (FK → `defense_period.id_defense`) | Đợt bảo vệ áp dụng đề tài. |
| `file_data` | File đính kèm đề tài, lưu BLOB trong MySQL. |

### `topic_supervisor`

Phân công giảng viên hướng dẫn cho đề tài.

| Trường | Ý nghĩa |
|---|---|
| `id_super_visor` (PK) | Mã phân công hướng dẫn. |
| `assigned_at` | Thời điểm phân công. |
| `ended_at` | Thời điểm kết thúc phân công. |
| `note` | Ghi chú. |
| `status` | Trạng thái hiệu lực. |
| `supervisor_role` | Vai trò: hướng dẫn chính hoặc đồng hướng dẫn. |
| `assigned_by` (FK → `user.user_id`) | Người thực hiện phân công. |
| `lecture_id` (FK → `lecture.lecture_id`) | Giảng viên được phân công. |
| `id_topic` (FK → `topic.id_topic`) | Đề tài được hướng dẫn. |

### `topic_registration`

Nguyện vọng/đăng ký đề tài của nhóm trong một đợt ghi danh.

| Trường | Ý nghĩa |
|---|---|
| `registration_id` (PK) | Mã đăng ký. |
| `note` | Ghi chú của nhóm. |
| `priority` | Thứ tự ưu tiên nguyện vọng. |
| `rejection_reason` | Lý do từ chối. |
| `reviewed_at` | Thời điểm xét duyệt. |
| `status` | Trạng thái đăng ký. |
| `submitted_at` | Thời điểm gửi đăng ký. |
| `enrollment_id` (FK → `graduation_enrollment.enrollment_id`) | Hồ sơ ghi danh liên quan. |
| `preferred_supervisor_id` (FK → `lecture.lecture_id`) | Giảng viên hướng dẫn mong muốn. |
| `reviewed_by` (FK → `user.user_id`) | Người duyệt đăng ký. |
| `id_team` (FK → `team.id_team`) | Nhóm đăng ký. |
| `id_topic` (FK → `topic.id_topic`) | Đề tài được đăng ký. |

### `library_topic`

Kho đề tài tham khảo, không phải đề tài thực hiện chính thức.

| Trường | Ý nghĩa |
|---|---|
| `id_library_topic` (PK) | Mã mục thư viện. |
| `description` | Mô tả. |
| `objective` | Mục tiêu tham khảo. |
| `technology` | Công nghệ gợi ý. |
| `title` | Tên đề tài tham khảo. |

## 4. Mốc tiến độ và bài nộp

### `miles_stone`

Mốc tiến độ thuộc một đợt bảo vệ. Sinh viên chỉ thấy mốc của đợt mà nhóm mình tham gia.

| Trường | Ý nghĩa |
|---|---|
| `id_miles_stone` (PK) | Mã mốc. |
| `description` | Hướng dẫn/mô tả mốc. |
| `allow_late_submission` | Có cho phép nộp muộn hay không. |
| `allowed_file_types` | Danh sách phần mở rộng được phép. |
| `deadline` | Hạn cuối nộp. |
| `max_file_size` | Dung lượng file tối đa (byte). |
| `milestone_name` | Tên mốc. |
| `milestone_type` | Loại mốc: phiếu giao đề tài, nhật ký, báo cáo, slide, mã nguồn... |
| `required` | Có bắt buộc nộp hay không. |
| `start_at` | Thời điểm mở nhận bài. |
| `status` | Nháp, mở, đóng hoặc hủy. |
| `id_defense` (FK → `defense_period.id_defense`) | Đợt bảo vệ sở hữu mốc. |

### `submistion`

Lưu từng phiên bản bài nộp của nhóm theo mốc tiến độ. Tên bảng giữ nguyên cách viết hiện có trong hệ thống.

| Trường | Ý nghĩa |
|---|---|
| `id_submission` (PK) | Mã bài nộp. |
| `checksum` | SHA-256 để nhận diện nội dung file. |
| `content_type` | MIME type của file. |
| `file_name` | Tên file gốc hiển thị cho người dùng. |
| `file_size` | Kích thước file theo byte. |
| `file_data` | Nội dung file BLOB lưu trực tiếp trong MySQL. |
| `is_late` | Đánh dấu bài nộp muộn. |
| `note` | Ghi chú khi nộp. |
| `status` | Trạng thái duyệt bài nộp. |
| `submitted_at` | Thời điểm nộp. |
| `updated_at` | Thời điểm cập nhật trạng thái. |
| `version` | Phiên bản nộp theo cùng nhóm và mốc. |
| `id_miles_stone` (FK → `miles_stone.id_miles_stone`) | Mốc tiến độ. |
| `submitted_by` (FK → `student.id_student`) | Sinh viên thao tác nộp. |
| `id_team` (FK → `team.id_team`) | Nhóm sở hữu bài nộp. |

### `comment`

Nhận xét trên bài nộp, bao gồm nhận xét thường và nhận xét của luồng duyệt.

| Trường | Ý nghĩa |
|---|---|
| `id_comment` (PK) | Mã nhận xét. |
| `comment_type` | Loại: nhận xét, yêu cầu sửa, phê duyệt hoặc từ chối. |
| `content` | Nội dung nhận xét. |
| `created_at` | Thời điểm tạo. |
| `deleted_at` | Thời điểm xóa mềm, nếu có. |
| `edited` | Cờ cho biết nội dung đã chỉnh sửa. |
| `updated_at` | Thời điểm sửa gần nhất. |
| `created_by` (FK → `user.user_id`) | Người viết nhận xét. |
| `id_submission` (FK → `submistion.id_submission`) | Bài nộp được nhận xét. |

## 5. Phản biện, điểm, hội đồng và lịch bảo vệ

### `review_assignment`

Phân công phản biện một đề tài cho giảng viên.

| Trường | Ý nghĩa |
|---|---|
| `id_review` (PK) | Mã phân công phản biện. |
| `asigned_date` | Thời điểm phân công (tên cột hiện có bị thiếu chữ `s`). |
| `cancelled_at` | Thời điểm hủy. |
| `cancelled_reason` | Lý do hủy. |
| `deadline` | Hạn phản biện. |
| `note` | Ghi chú phân công. |
| `recommendation` | Kết luận đủ điều kiện, cần sửa hoặc không đủ điều kiện bảo vệ. |
| `review_comment` | Nội dung phản biện. |
| `reviewed_at` | Thời điểm hoàn tất phản biện. |
| `status` | Trạng thái luồng phản biện. |
| `submitted_at` | Thời điểm nộp phản biện. |
| `assigned_by` (FK → `user.user_id`) | Người phân công. |
| `lecture_id` (FK → `lecture.lecture_id`) | Giảng viên phản biện. |
| `reviewed_by` (FK → `user.user_id`) | Người xác nhận/duyệt phản biện. |
| `id_topic` (FK → `topic.id_topic`) | Đề tài bị phản biện. |

### `score`

Điểm và nhận xét của giảng viên cho sinh viên theo đề tài.

| Trường | Ý nghĩa |
|---|---|
| `score_id` (PK) | Mã bản ghi điểm. |
| `comment` | Nhận xét khi chấm. |
| `created_at` | Thời điểm tạo. |
| `published_at` | Thời điểm công bố. |
| `score` | Giá trị điểm. |
| `score_type` | Loại điểm: tổng kết, hướng dẫn, phản biện hoặc hội đồng. |
| `status` | Nháp, đã nộp hoặc đã khóa. |
| `submitted_at` | Thời điểm giảng viên gửi điểm. |
| `updated_at` | Thời điểm chỉnh sửa gần nhất. |
| `lecture_id` (FK → `lecture.lecture_id`) | Giảng viên chấm. |
| `id_student` (FK → `student.id_student`) | Sinh viên được chấm. |
| `id_topic` (FK → `topic.id_topic`) | Đề tài liên quan. |

### `defense_comittees`

Hội đồng bảo vệ của một đợt. Tên bảng hiện có viết `comittees`.

| Trường | Ý nghĩa |
|---|---|
| `id_comittees` (PK) | Mã hội đồng. |
| `activated_at` | Thời điểm kích hoạt hội đồng. |
| `comittees_name` | Tên hội đồng. |
| `created_at` | Thời điểm tạo. |
| `deactivation_reason` | Lý do ngừng hoạt động. |
| `description` | Mô tả/ghi chú. |
| `status` | Nháp, hoạt động hoặc không hoạt động. |
| `updated_at` | Thời điểm cập nhật. |
| `created_by` (FK → `user.user_id`) | Người tạo. |
| `id_defense` (FK → `defense_period.id_defense`) | Đợt bảo vệ. |

### `comittees_member`

Thành viên hội đồng bảo vệ.

| Trường | Ý nghĩa |
|---|---|
| `comittees_member_id` (PK) | Mã thành viên hội đồng. |
| `assigned_at` | Thời điểm thêm thành viên. |
| `ended_at` | Thời điểm kết thúc tham gia. |
| `note` | Ghi chú. |
| `role` | Vai trò: chủ tịch, thư ký, phản biện hoặc thành viên. |
| `status` | Trạng thái hiệu lực. |
| `assigned_by` (FK → `user.user_id`) | Người phân công. |
| `id_comittees` (FK → `defense_comittees.id_comittees`) | Hội đồng. |
| `lecture_id` (FK → `lecture.lecture_id`) | Giảng viên thành viên. |

### `defense_schedules`

Lịch bảo vệ của một đề tài trước hội đồng.

| Trường | Ý nghĩa |
|---|---|
| `id_defense_scheduce` (PK) | Mã lịch bảo vệ (tên cột hiện có bị viết sai `scheduce`). |
| `cancelled_at` | Thời điểm hủy. |
| `cancelled_reason` | Lý do hủy. |
| `created_at` | Thời điểm tạo. |
| `defense_date` | Ngày bảo vệ. |
| `end_time` | Giờ kết thúc. |
| `location` | Địa điểm/cơ sở. |
| `note` | Ghi chú. |
| `postponed_at` | Thời điểm hoãn. |
| `postponed_reason` | Lý do hoãn. |
| `published_at` | Thời điểm công bố. |
| `room` | Phòng bảo vệ. |
| `sesstion` | Buổi bảo vệ: sáng, chiều hoặc tối (tên cột hiện có bị viết sai). |
| `start_time` | Giờ bắt đầu. |
| `status` | Trạng thái lịch. |
| `updated_at` | Thời điểm cập nhật. |
| `created_by` (FK → `user.user_id`) | Người lập lịch. |
| `id_comittees` (FK → `defense_comittees.id_comittees`) | Hội đồng bảo vệ. |
| `id_topic` (UNIQUE, FK → `topic.id_topic`) | Đề tài được xếp lịch; mỗi đề tài có tối đa một lịch. |

### `defense_schedule_history`

Lịch sử thay đổi của lịch bảo vệ để truy vết.

| Trường | Ý nghĩa |
|---|---|
| `history_id` (PK) | Mã bản ghi lịch sử. |
| `action` | Hành động tạo, cập nhật, công bố, hoãn, đổi lịch, hoàn thành hoặc hủy. |
| `changed_at` | Thời điểm thay đổi. |
| `new_committee_id`, `new_committee_name` | Hội đồng mới sau thay đổi. |
| `new_defense_date`, `new_start_time`, `new_end_time` | Ngày và khung giờ mới. |
| `new_location`, `new_room` | Địa điểm/phòng mới. |
| `new_status` | Trạng thái mới. |
| `old_committee_id`, `old_committee_name` | Hội đồng cũ. |
| `old_defense_date`, `old_start_time`, `old_end_time` | Ngày và khung giờ cũ. |
| `old_location`, `old_room` | Địa điểm/phòng cũ. |
| `previous_status` | Trạng thái trước thay đổi. |
| `reason` | Lý do thay đổi. |
| `changed_by` (FK → `user.user_id`) | Người thay đổi. |
| `schedule_id` (FK → `defense_schedules.id_defense_scheduce`) | Lịch được ghi nhận lịch sử. |

## 6. Biểu mẫu

### `template`

Biểu mẫu để tải về như phiếu giao đề tài, mẫu báo cáo, phiếu chấm. File được lưu BLOB trong MySQL.

| Trường | Ý nghĩa |
|---|---|
| `template_id` (PK) | Mã biểu mẫu. |
| `content_type` | MIME type file. |
| `create_at` | Ngày tạo. |
| `description` | Mô tả biểu mẫu. |
| `file_size` | Kích thước file. |
| `original_file_name` | Tên file gốc. |
| `status` | Hoạt động, không hoạt động hoặc lưu trữ. |
| `template_name` | Tên biểu mẫu. |
| `template_type` | Loại biểu mẫu. |
| `updated_at` | Thời điểm cập nhật. |
| `version` | Phiên bản biểu mẫu. |
| `uploaded_by` (FK → `user.user_id`) | Người upload. |
| `file_data` | Nội dung file BLOB. |

## 7. Các bảng sequence

Các bảng sau có cùng một trường `next_val`, dùng bởi Hibernate để cấp ID cho bảng nghiệp vụ tương ứng; không chứa dữ liệu nghiệp vụ:

| Bảng sequence | Bảng dùng ID |
|---|---|
| `academic_year_seq` | `academic_year` |
| `class_seq` | `class` |
| `comittees_member_seq` | `comittees_member` |
| `comment_seq` | `comment` |
| `defense_comittees_seq` | `defense_comittees` |
| `defense_period_seq` | `defense_period` |
| `defense_schedule_history_seq` | `defense_schedule_history` |
| `defense_schedules_seq` | `defense_schedules` |
| `graduation_enrollment_seq` | `graduation_enrollment` |
| `library_topic_seq` | `library_topic` |
| `major_seq` | `major` |
| `miles_stone_seq` | `miles_stone` |
| `review_assignment_seq` | `review_assignment` |
| `score_seq` | `score` |
| `submistion_seq` | `submistion` |
| `team_seq` | `team` |
| `template_seq` | `template` |
| `topic_registration_seq` | `topic_registration` |
| `topic_seq` | `topic` |
| `topic_supervisor_seq` | `topic_supervisor` |

## 8. MongoDB

Database MongoDB `graduration_managerment` dùng cho dữ liệu phụ trợ, không thay thế MySQL cho nghiệp vụ chính.

| Collection | Chức năng |
|---|---|
| `notification` | Thông báo trong hệ thống cho người dùng. |
| `email` | Hàng đợi/lịch sử gửi email. |
| `AuditLog` | Nhật ký thao tác phục vụ truy vết. |
