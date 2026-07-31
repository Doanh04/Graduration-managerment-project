package com.graduration.Constain;

public enum PermissionConstain {
//    Quản lý người dùng
    user_read, //Xem danh sách, thông tin chi tiết giảng viên/sinh viên.
    user_create, // Thêm mới tài khoản (thủ công hoặc import Excel).
    user_update,//Chỉnh sửa thông tin cá nhân, đổi trạng thái tài khoản (Active/Locked).
    user_delete, //Xóa tài khoản khỏi hệ thống.
    user_assign_role, //Gán vai trò hoặc phân quyền cho người dùng.

//    Module: Đợt đồ án / Cấu hình hệ thống
    semester_read, //Xem danh sách các đợt/năm học làm đồ án.
    semester_create, // Tạo đợt đồ án mới (ví dụ: Đợt 1 năm học 2025-2026).
    semeste_update, // Cấu hình thời gian các mốc (thời gian đăng ký, thời gian nộp báo cáo, thời gian bảo vệ).
    semester_delete, // Xóa đợt đồ án.

//    Module: Đề tài đồ án
    topic_read, // Xem danh sách đề tài đang mở, chi tiết mô tả đề tài.
    topi_create, // Tạo đề tài mới (Giảng viên tạo hoặc Sinh viên đề xuất).
    topic_update, // Chỉnh sửa thông tin đề tài (chỉ sửa được khi chưa duyệt hoặc do chủ sở hữu sửa).
    topic_delete, // Xóa đề tài.
    topic_approve, // Phê duyệt hoặc từ chối đề tài (Dành cho Trưởng khoa/Hội đồng hoặc GVHD).
    topic_register, // Đăng ký chọn đề tài (Dành cho Sinh viên).

//    Module: Phân công hướng dẫn
    assignment_read, // Xem danh sách phân công GVHD - Sinh viên.
    assignment_create, // Trưởng khoa thực hiện phân công GVHD cho sinh viên.
    assignment_update, //Thay đổi, điều chỉnh phân công GVHD.
    assignment_delete, // Hủy phân công.

//    Module: Tiến độ & Nhật ký công việc
    progress_read, // Xem nhật ký công việc, tiến độ hàng tuần của sinh viên.
    progress_create, // Thêm mới nhật ký công việc, báo cáo tuần (Sinh viên).
    progress_comment, // Giảng viên viết nhận xét, đánh giá vào nhật ký tiến độ của sinh viên.

//    Module: Nộp sản phẩm & Tài liệu
    council_read, //Xem danh sách hội đồng, lịch bảo vệ, phân công tiểu ban.
    council_create, //Thành lập hội đồng bảo vệ, gán danh sách đồ án vào hội đồng.
    council_update, // Thay đổi thành viên hội đồng hoặc lịch bảo vệ.
    council_delete, //xóa hội đồng.

//    Module: Chấm điểm
    score_read, // Xem điểm số các thành phần.
    score_evaluate_advisor, // Giảng viên hướng dẫn chấm điểm quá trình/hướng dẫn.
    score_evaluate_reviewer, // Giảng viên phản biện nhập điểm và nhận xét.
    score_evaluate_council,// Thành viên hội đồng nhập điểm bảo vệ trực tiếp.
    score_publish //công bố bảng điểm tổng kết chính thức ra hệ thống.
}
