# Sơ lược kế hoạch xây dựng UI

## 1. Mục tiêu

Xây dựng giao diện website quản lý học phần Đồ án tốt nghiệp cho khoa Công nghệ thông tin, Trường Đại học Công nghiệp Việt – Hung. UI phải hỗ trợ ba nhóm người dùng chính:

- Ban quản lý khoa/Admin.
- Giảng viên.
- Sinh viên.

Giao diện được xây dựng dựa trên API và các trạng thái nghiệp vụ hiện có trong backend. Chức năng gửi email tạm thời chưa triển khai và sẽ được thiết kế lại sau.

## 2. Hiện trạng frontend

- Công nghệ: React 19, Vite, React Router, Axios và SCSS.
- Đã có giao diện đăng nhập responsive.
- Đã có `AuthContext`, đăng nhập, đăng xuất và khôi phục phiên bằng HttpOnly Cookie.
- Chưa có dashboard và các trang nghiệp vụ.
- Router hiện mới điều hướng về trang đăng nhập.
- Cần đồng bộ trường đăng nhập `identifier` giữa UI và authentication service.
- Cần sử dụng một HTTP client tập trung và xử lý refresh token thống nhất.

## 3. Nguyên tắc thiết kế UI

- Hiển thị đúng chức năng theo `accountType`, role và permission.
- Giao diện nhất quán giữa Admin, giảng viên và sinh viên.
- Sử dụng ảnh `client-react/graduration-client/src/img/sv_logo_dashboard.png` làm logo chính thức trên toàn bộ giao diện.
- Không thay logo chính thức bằng logo SVG tự dựng; component hiển thị logo phải dùng trực tiếp ảnh trên.
- Responsive trên máy tính, máy tính bảng và điện thoại.
- Các bảng dữ liệu hỗ trợ tìm kiếm, lọc, phân trang và trạng thái tải.
- Form có validation, thông báo lỗi rõ ràng và xác nhận trước thao tác quan trọng.
- Trạng thái nghiệp vụ được thể hiện bằng badge, màu sắc và action phù hợp.
- File tải lên phải hiển thị tên, loại, dung lượng, tiến trình và kết quả upload.
- Các trang cần có trạng thái loading, empty, error và permission denied.
- Không đưa secret hoặc token vào local storage; xác thực tiếp tục dùng HttpOnly Cookie.

## 4. Khung giao diện chung

### 4.1. Public layout

- Trang đăng nhập hiện có, sử dụng logo `sv_logo_dashboard.png`.
- Trang không có quyền truy cập.
- Trang không tìm thấy.

### 4.2. Application layout

- Sidebar thay đổi theo vai trò và hiển thị logo chính thức từ `src/img/sv_logo_dashboard.png`.
- Header gồm tiêu đề trang, thông tin người dùng và đăng xuất; có thể dùng phiên bản thu gọn của cùng logo khi phù hợp với kích thước màn hình.
- Breadcrumb cho các màn hình nhiều cấp.
- Khu vực thông báo trên web.
- Nội dung chính dùng chung hệ thống card, table, form, modal và badge.

## 5. Giao diện Admin/Ban quản lý khoa

### 5.1. Dashboard tổng quan

- Số lượng sinh viên, giảng viên, nhóm và đề tài.
- Tiến độ nộp báo cáo theo từng milestone.
- Danh sách bài nộp trễ hạn hoặc chưa nộp.
- Tình trạng đề tài, hội đồng và lịch bảo vệ.
- Các công việc cần xử lý gần nhất.

### 5.2. Quản lý tài khoản và phân quyền

- CRUD sinh viên.
- CRUD giảng viên.
- Import danh sách từ Excel.
- Reset mật khẩu và khóa/ngừng hoạt động tài khoản.
- Quản lý role và permission.

### 5.3. Quản lý dữ liệu đào tạo

- Ngành học.
- Lớp học.
- Năm học.
- Đợt bảo vệ.

### 5.4. Quản lý đề tài và nhóm

- CRUD đề tài.
- Duyệt hoặc từ chối đề tài.
- CRUD nhóm sinh viên.
- Thêm, xóa hoặc import sinh viên vào nhóm.
- Gán đề tài cho nhóm.
- Xem phần công việc và kết quả riêng của từng sinh viên.

### 5.5. Phân công

- Phân công giảng viên hướng dẫn.
- Phân công giảng viên phản biện.
- Theo dõi tải phân công của từng giảng viên.
- Hủy hoặc cập nhật phân công.

### 5.6. Hội đồng và lịch bảo vệ

- Tạo và quản lý hội đồng.
- Phân công thành viên, chủ tịch, thư ký và phản biện.
- Kiểm tra tính hợp lệ của hội đồng.
- Tạo lịch bảo vệ theo thời gian, phòng, đề tài và hội đồng.
- Hiển thị và xử lý xung đột lịch.
- Theo dõi lịch sử thay đổi lịch bảo vệ.

### 5.7. Tiến độ, biểu mẫu và điểm

- Tạo và quản lý milestone/deadline.
- Theo dõi bài nộp đúng hạn, trễ hạn và chưa nộp.
- Quản lý tiêu chí chấm điểm.
- Theo dõi, khóa hoặc mở khóa điểm.
- Quản lý biểu mẫu và thư viện đề tài.
- Xem audit log.

## 6. Giao diện Giảng viên

- Dashboard công việc và deadline sắp tới.
- Danh sách đề tài/sinh viên đang hướng dẫn.
- Xem các phiên bản bài nộp của nhóm.
- Nhận xét, yêu cầu chỉnh sửa, phê duyệt hoặc từ chối báo cáo.
- Danh sách đề tài được phân công phản biện.
- Nhập kết quả phản biện và đề xuất đánh giá.
- Xem lịch tham gia hội đồng.
- Chấm điểm theo tiêu chí và gửi điểm.
- Xem lại lịch sử nhận xét, phản biện và chấm điểm.

## 7. Giao diện Sinh viên

- Dashboard cá nhân với deadline và trạng thái tiến độ.
- Xem thông tin nhóm và thành viên.
- Tra cứu danh sách đề tài.
- Đăng ký/chọn đề tài hoặc đề xuất đề tài cá nhân khi được phép.
- Đăng ký nguyện vọng giảng viên hướng dẫn.
- Xem milestone và yêu cầu của từng mốc.
- Upload phiếu đăng ký, nhật ký, đề cương, báo cáo và slide bảo vệ.
- Xem lịch sử phiên bản, trạng thái nộp và dấu hiệu nộp muộn.
- Xem nhận xét và phản hồi từ giảng viên.
- Xem lịch bảo vệ, hội đồng, địa điểm và phòng.
- Xem điểm khi điểm đã được công bố.
- Xem và tải biểu mẫu.
- Khai báo rõ phần công việc và kết quả của từng sinh viên trong nhóm.

## 8. Luồng trạng thái cần thể hiện trên UI

### 8.1. Đề tài

`DRAFT → PENDING_APPROVAL → APPROVED hoặc REJECTED → REGISTERED`

### 8.2. Milestone

`DRAFT → OPEN → CLOSED`

Milestone ở trạng thái `DRAFT` hoặc `OPEN` có thể chuyển sang `CANCELLED` theo quy tắc backend.

### 8.3. Bài nộp

`SUBMITTED → UNDER_REVIEW → APPROVED, REJECTED hoặc REVISION_REQUIRED`

Sinh viên có thể rút bài theo điều kiện backend. Mỗi lần nộp lại được hiển thị thành một phiên bản riêng và phải đánh dấu nếu nộp sau deadline.

### 8.4. Phản biện

`ASSIGNED → IN_PROGRESS → SUBMITTED → APPROVED hoặc REVISION_REQUIRED`

Phân công có thể chuyển sang `CANCELLED` khi đáp ứng điều kiện nghiệp vụ.

### 8.5. Hội đồng

`DRAFT → ACTIVE → INACTIVE`

### 8.6. Điểm

`DRAFT → SUBMITTED → LOCKED`

Admin có thể mở khóa điểm từ `LOCKED` về `DRAFT` theo API hiện tại.

## 9. Cấu trúc frontend dự kiến

```text
src/
├── assets/
├── components/
│   ├── common/
│   ├── feedback/
│   ├── forms/
│   ├── navigation/
│   └── tables/
├── config/
├── context/
├── hooks/
├── layouts/
├── pages/
│   ├── admin/
│   ├── lecturer/
│   ├── student/
│   └── shared/
├── routes/
├── services/
├── styles/
└── utils/
```

Tên thư mục cũ sẽ được chuyển đổi có kiểm soát để tránh làm hỏng luồng đăng nhập đang có.

## 10. Thứ tự triển khai

### Giai đoạn 1: Nền tảng UI

- Sửa kết nối đăng nhập và điều hướng sau đăng nhập.
- Chuẩn hóa HTTP client và refresh token.
- Tạo route guard theo account type/role.
- Tạo application layout, sidebar, header và component dùng chung với logo `sv_logo_dashboard.png`.
- Tạo trang lỗi, loading và empty state.

### Giai đoạn 2: Admin

- Dashboard.
- Sinh viên, giảng viên, role và permission.
- Ngành, lớp, năm học và đợt bảo vệ.
- Đề tài, nhóm và phân công hướng dẫn/phản biện.
- Milestone, bài nộp, hội đồng, lịch bảo vệ và điểm.
- Biểu mẫu, thư viện đề tài và audit log.

### Giai đoạn 3: Giảng viên

- Dashboard giảng viên.
- Hướng dẫn và duyệt báo cáo.
- Phản biện.
- Hội đồng và chấm điểm.

### Giai đoạn 4: Sinh viên

- Dashboard sinh viên.
- Nhóm, đề tài và nguyện vọng hướng dẫn.
- Milestone, upload và lịch sử bài nộp.
- Nhận xét, lịch bảo vệ, biểu mẫu và điểm.

### Giai đoạn 5: Hoàn thiện

- Responsive và accessibility.
- Kiểm tra quyền trên toàn bộ route/action.
- Kiểm thử các trạng thái loading, empty và error.
- Tối ưu trải nghiệm upload và bảng dữ liệu.
- Chuẩn bị cấu hình triển khai cho tên miền `itf.viu.edu.vn`.

## 11. Phạm vi tạm hoãn

- Gửi email và thiết kế lại luồng email.
- Cấu hình máy chủ production và DNS thực tế.
- Các tính năng chưa có API backend tương ứng sẽ chỉ dựng giao diện mẫu sau khi được thống nhất.

## 12. Tiêu chí hoàn thành UI

- Người dùng đăng nhập và được chuyển đúng khu vực theo quyền.
- Không thể truy cập route hoặc action ngoài quyền được cấp.
- Các chức năng chính gọi đúng API backend và phản ánh đúng trạng thái nghiệp vụ.
- Mọi bảng/form chính hoạt động tốt trên desktop và thiết bị nhỏ.
- Sinh viên nhìn thấy deadline, trạng thái nộp muộn và phản hồi rõ ràng.
- Giảng viên theo dõi, nhận xét và chấm điểm được ngay trên hệ thống.
- Admin theo dõi toàn bộ tiến độ và quản lý các dữ liệu cốt lõi từ một giao diện thống nhất.
