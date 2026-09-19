# Luồng hoạt động của Admin

Tài liệu này mô tả luồng nghiệp vụ của **Ban quản lý khoa** trong hệ thống quản lý đồ án tốt nghiệp. Admin là người thiết lập dữ liệu đầu vào, tổ chức việc đăng ký đề tài, phân công giảng viên, lập hội đồng và lịch bảo vệ.

> Lưu ý: route `/admin` hiện cho phép tài khoản có vai trò `ADMIN` hoặc `FACULTY` truy cập. Một số chức năng nhạy cảm, đặc biệt **Nhật ký hoạt động**, chỉ dành cho `ADMIN`.

## 1. Sơ đồ tổng quát

```text
Đăng nhập
   ↓
Thiết lập năm học và đợt bảo vệ
   ↓
Tạo dữ liệu khoa: ngành, lớp, sinh viên, giảng viên
   ↓
Tạo nhóm và quản lý thành viên
   ↓
Tạo/tiếp nhận đề tài
   ↓
Duyệt đề tài hoặc duyệt đăng ký đề tài sinh viên
   ↓
Gán đề tài cho nhóm
   ↓
Phân công giảng viên hướng dẫn và phản biện
   ↓
Tạo milestone → mở milestone → theo dõi bài nộp/nhận xét
   ↓
Ghi danh sinh viên vào đợt bảo vệ
   ↓
Tạo hội đồng → thêm thành viên → kích hoạt hội đồng
   ↓
Lập lịch bảo vệ → kiểm tra xung đột → công bố lịch
   ↓
Hoàn thành, tạm hoãn/hủy khi cần → xem lịch sử thay đổi
```

## 2. Đăng nhập, phiên và quyền truy cập (frontend + backend)

### 2.1. Luồng đăng nhập thành công

Frontend hiện dùng `POST /auth/cookie/login` và gửi `{ identifier, password }` với
`withCredentials=true`. `identifier` có thể là tên đăng nhập, mã giảng viên hoặc mã
sinh viên.

```text
Form đăng nhập
   ↓
POST /auth/cookie/login
   ↓
AuthenticationService.authenticate()
   ├─ Chuẩn hóa identifier
   ├─ Tìm UserEntity theo username/mã giảng viên/mã sinh viên
   ├─ Đối chiếu mật khẩu BCrypt
   ├─ Kiểm tra tài khoản ACTIVE
   └─ Tổng hợp role và permission
   ↓
Tạo access JWT + refresh JWT
   ↓
Set-Cookie: access_token, refresh_token (HttpOnly, SameSite=Strict)
   ↓
Frontend gọi GET /auth/cookie/me, lưu thông tin phiên trong AuthContext
   ↓
Điều hướng tới dashboard phù hợp với accountType/role
```

Khi thành công, backend trả HTTP `200` và thông tin `authenticated=true`, username,
họ tên, loại tài khoản và danh sách role. Access token chứa `ROLE_*` và
`PERMISSION_*` trong claim `scope`; refresh token không chứa các quyền này và chỉ
dùng để cấp lại cặp token.

### 2.2. Các trường hợp đăng nhập thất bại

Backend ném `AppException`, sau đó `GlobalExceptionHandler` chuyển thành JSON dạng
`{ code, message }`. Frontend dùng mã lỗi để hiển thị thông báo tiếng Việt.

| Điều kiện | Mã | HTTP | Kết quả |
| --- | ---: | :---: | --- |
| Không nhập tên đăng nhập/mã sinh viên/mã giảng viên | `1025` `INVALID_USERNAME` | 400 | Yêu cầu nhập thông tin định danh |
| Không nhập mật khẩu | `1027` `PASSWORD_NOT_BLANK` | 400 | Yêu cầu nhập mật khẩu |
| Không tìm thấy tài khoản phù hợp | `1021` `USER_NOT_FOUND` | 404 | Không tồn tại tài khoản theo identifier |
| Mật khẩu không khớp | `1020` `UNAUTHORIZED` | 403 | Tên đăng nhập hoặc mật khẩu không chính xác |
| Identifier khớp nhiều tài khoản có cùng mật khẩu | `1040` `AMBIGUOUS_LOGIN_IDENTIFIER` | 409 | Yêu cầu dùng identifier khác để tránh đăng nhập nhầm |
| Tài khoản không ở trạng thái `ACTIVE` | `1073` `ACCOUNT_INACTIVE` | 401 | Tài khoản đã dừng hoạt động |
| Dữ liệu JSON không đọc được/không đúng kiểu | `1012` `INVALID_KEY` | 400 | Request không hợp lệ |
| Lỗi không được phân loại | `9999` `UKNOWN_ERROR` | 500 | Hệ thống ghi log và yêu cầu thử lại |

Do mã `UNAUTHORIZED` hiện được khai báo HTTP 403 trong backend, frontend phải dựa
vào cả `code` và `message`, không nên chỉ kiểm tra status để phân biệt sai mật khẩu
với thiếu quyền.

### 2.3. Kiểm tra phiên ở các API bảo vệ

Sau đăng nhập, mọi request nghiệp vụ gửi kèm cookie `access_token` (hoặc Bearer
token nếu gọi trực tiếp). `SercurityConfig` xử lý theo thứ tự:

1. `BearerTokenResolver` đọc Authorization header; nếu không có thì đọc cookie
   `access_token`.
2. JWT decoder kiểm tra chữ ký HS256, issuer/thời hạn và loại token `ACCESS`.
3. Token đã ghi trong bảng `invalidated` bị từ chối.
4. Spring Security tạo `Authentication` từ claim `scope`.
5. `@PreAuthorize`/quy tắc service kiểm tra role hoặc permission cụ thể.
6. Controller nhận request, service thực hiện nghiệp vụ và repository đọc/ghi CSDL.

Nếu không có token, token hết hạn/sai chữ ký/sai loại hoặc đã bị thu hồi, entry point
trả HTTP `401`, mã `1019` (`UNAUTHENTICATED`). Nếu token hợp lệ nhưng role không có
quyền gọi endpoint, access-denied handler trả HTTP `403`, mã `1026`
(`ACCESS_DENIED`). Frontend không được tự bỏ qua hai bước kiểm tra này.

### 2.4. Khôi phục và làm mới phiên

Khi tải lại trang, `AuthContext` kiểm tra marker phiên trong localStorage rồi gọi
`POST /auth/cookie/refresh`. Backend xác minh refresh token, kiểm tra token chưa bị
thu hồi và tài khoản còn `ACTIVE`, sau đó:

1. Ghi refresh token cũ vào bảng token vô hiệu hóa (token rotation).
2. Phát hành access token và refresh token mới.
3. Ghi đè hai HttpOnly Cookie.
4. Frontend gọi `GET /auth/cookie/me` để lấy role/accountType mới nhất.

Nếu refresh thất bại, frontend xóa marker phiên, xóa user trong `AuthContext` và
đưa người dùng về màn hình đăng nhập. `POST /auth/cookie/introspect` chỉ dùng để
kiểm tra access token còn hợp lệ; kết quả `valid=false` không cấp thêm quyền.

### 2.5. Đăng xuất

`POST /auth/cookie/logout` nhận các cookie hiện có (nếu còn), ghi access/refresh
token hợp lệ vào danh sách vô hiệu hóa và gửi cookie có `Max-Age=0` để xóa trên
trình duyệt. Frontend luôn xóa trạng thái local kể cả khi request logout gặp lỗi.

### 2.6. Vòng đời xử lý một API nghiệp vụ

```text
HTTP request + Cookie
   → CORS/CSRF + JWT resource filter
   → xác thực Authentication
   → @PreAuthorize / kiểm tra permission
   → Controller nhận DTO và validate @Valid
   → Service @Transactional kiểm tra quy tắc nghiệp vụ
   → Repository thao tác MySQL/MongoDB/file storage
   → Mapper tạo ApiResponse { code, message, result }
   → Frontend cập nhật bảng, modal, notification
```

Nhánh lỗi đi theo chiều ngược lại: lỗi validate, lỗi nghiệp vụ (`AppException`),
lỗi quyền hoặc lỗi CSDL được handler chuyển thành response thống nhất. Vì vậy UI
phải dùng `code` để dịch lỗi (ví dụ đề tài chưa đủ điều kiện, đợt đã kết thúc,
trùng dữ liệu), không hiển thị nguyên văn thông báo tiếng Anh từ exception.

### 2.7. Giao dịch và nhật ký hoạt động

Các thao tác ghi dữ liệu chính chạy trong transaction. Khi transaction commit thành
công, `AuditTrailAspect` đăng ký callback `afterCommit` để lưu audit log; lỗi lưu
audit được ghi cảnh báo và không được làm thay đổi kết quả nghiệp vụ đã commit.
Nếu transaction rollback, thay đổi nghiệp vụ không được coi là thành công và không
được phát thông báo thành công giả ở frontend.

### 2.8. Các endpoint xác thực đang có

| Endpoint | Mục đích | Kết quả chính |
| --- | --- | --- |
| `POST /auth/cookie/login` | Đăng nhập theo cơ chế frontend đang dùng | Đặt hai HttpOnly Cookie và trả hồ sơ người dùng |
| `POST /auth/cookie/refresh` | Cấp lại phiên khi access token hết hạn | Thu hồi refresh cũ, đặt cặp Cookie mới |
| `POST /auth/cookie/introspect` | Kiểm tra access token trong cookie | Trả `valid=true/false` |
| `GET /auth/cookie/me` | Lấy người dùng hiện tại từ subject của JWT | Trả hồ sơ và role mới nhất |
| `POST /auth/cookie/logout` | Kết thúc phiên cookie | Vô hiệu hóa token và xóa Cookie |
| `POST /auth/login`, `/auth/refresh`, `/auth/logout` | API dạng JSON tương thích cho client không dùng cookie | Token nằm trong response body; phải tự bảo quản token |

Trong cấu hình hiện tại, frontend React gọi nhóm `/auth/cookie/*`; không trộn hai
cơ chế trong cùng một phiên. CORS cho phép origin `localhost:5173`/
`127.0.0.1:5173` và bật credentials để trình duyệt gửi cookie.

### 2.9. Phân tích các tình huống backend khác

#### A. Request không hợp lệ

Backend kiểm tra request trước khi chạy nghiệp vụ. Nếu thiếu trường bắt buộc, sai
kiểu dữ liệu, sai định dạng ngày/giờ hoặc file không đúng mẫu, service không được
ghi dữ liệu. `GlobalExceptionHandler` trả HTTP `400` với mã lỗi phù hợp (`1012`,
`1034`, `1053`, `1090`, `1155`...). UI phải giữ dữ liệu người dùng đã nhập, đánh
dấu đúng trường lỗi và chỉ đóng modal khi backend trả thành công.

#### B. Không tìm thấy dữ liệu

Khi id tham chiếu không tồn tại hoặc đã bị xóa, repository trả rỗng và service ném
lỗi `*_NOT_FOUND` (HTTP `404`). Ví dụ: năm học `1046`, đợt bảo vệ `1051`, đề tài
`1074`, nhóm `1041`, hội đồng `1134`, lịch `1149`, bài nộp `1096`, hồ sơ ghi danh
`1179`. UI cần tải lại danh sách và thông báo bản ghi không còn tồn tại, không tự
tạo bản ghi thay thế.

#### C. Trùng dữ liệu và request lặp

Các ràng buộc duy nhất và kiểm tra nghiệp vụ ngăn việc tạo trùng. Backend trả HTTP
`409`, thay vì coi request lặp là thành công giả:

- Năm học/đợt bảo vệ/ngành/lớp hoặc tên hội đồng đã tồn tại (`1049`, `1055`, `1144`).
- Tên đề tài trùng trong cùng đợt (`1078`), đề tài đã thuộc nhóm khác (`1045`) hoặc
  nhóm đã có đề tài (`1084`).
- Sinh viên đã thuộc nhóm khác (`1044`), giảng viên đã được gán (`1128`) hoặc
  phản biện cùng đề tài (`1165`).
- Nhóm đã có đăng ký chờ (`1175`) hoặc sinh viên đã ghi danh đợt đó (`1180`).

Nếu người dùng bấm nút hai lần, frontend nên khóa nút trong lúc chờ response; nếu
vẫn phát sinh request đồng thời, unique constraint và transaction backend là lớp
bảo vệ cuối cùng.

#### D. Bản ghi có tồn tại nhưng không được phép thao tác

Đây là khác biệt giữa `404` (không tồn tại) và `409` (tồn tại nhưng sai trạng thái
hoặc sai điều kiện). Một số ví dụ:

| Tình huống | Backend xử lý | Mã thường gặp |
| --- | --- | --- |
| Đợt bảo vệ đã kết thúc | Từ chối tạo/sửa đề tài, milestone, phân công hoặc lịch | `1082` |
| Đề tài chưa duyệt, chưa có nhóm hoặc chưa có hướng dẫn | Không cho phân công phản biện/lập lịch | `1083`, `1160` |
| Milestone chưa mở/chưa đến hạn/hết hạn không cho nộp muộn | Từ chối bài nộp | `1100`, `1101`, `1102` |
| Bài nộp mới nhất đã được duyệt | Không cho nộp phiên bản khác | `1104` |
| Hội đồng thiếu tối thiểu 3 người, Chủ tịch hoặc Thư ký | Không cho kích hoạt/lập lịch | `1145` |
| Lịch trùng phòng, hội đồng, đề tài hoặc thời gian | Rollback request tạo/cập nhật lịch | `1157` |
| Ghi danh chuyển trạng thái không theo vòng đời | Không cập nhật status | `1181` |

UI phải hiển thị nguyên nhân và tải lại trạng thái từ server; không chỉ đổi màu
badge cục bộ.

#### E. Kiểm tra quyền và phạm vi dữ liệu

`@PreAuthorize` chặn request ngay khi role không phù hợp; service tiếp tục kiểm tra
đối tượng hiện tại để tránh người dùng hợp lệ truy cập nhầm dữ liệu người khác.
Ví dụ:

- `ROLE_ADMIN` được xem AuditLog; tài khoản khác nhận `403/1026`.
- Sinh viên chỉ thao tác nhóm, đề tài đăng ký, milestone và bài nộp thuộc nhóm
  của mình; giảng viên chỉ xem/phản hồi đề tài được phân công.
- Admin/faculty có thể quản trị dữ liệu chung theo annotation của service, nhưng
  vẫn bị chặn bởi điều kiện trạng thái và quan hệ dữ liệu.
- Token hợp lệ không đồng nghĩa có quyền trên mọi module; frontend chỉ dùng quyền
  để ẩn/hiện UI, backend mới là nơi quyết định cuối cùng.

#### F. Import Excel và lưu file

Luồng import đọc từng dòng, chuẩn hóa mã định danh rồi đối chiếu dữ liệu tham chiếu
(lớp, ngành, đợt, nhóm). File rỗng/sai cột trả `1034`; dòng không hợp lệ trả
`1035` hoặc lỗi cụ thể; dữ liệu đã tồn tại trả `1173`. Nếu có lỗi, UI phải hiển thị
danh sách dòng lỗi và không báo “import thành công” cho toàn bộ file.

Với bài nộp, backend kiểm tra file bắt buộc, dung lượng, phần mở rộng và nơi lưu
trữ (`1097`–`1099`, `1109`). Nếu ghi file thành công nhưng ghi CSDL thất bại hoặc
ngược lại, service phải rollback/xử lý bù để không tạo bản ghi trỏ tới file không
tồn tại.

#### G. Transaction, rollback và thông báo

Một thao tác có nhiều bước (duyệt đề tài rồi gán nhóm, tạo lịch cho nhiều đề tài,
ghi danh nhiều sinh viên) chỉ được coi là thành công khi toàn bộ transaction commit.
Khi một bước thất bại, các bước trước đó bị rollback; frontend nhận lỗi và không
được hiển thị notification thành công. AuditLog được ghi sau commit nên không làm
cho nghiệp vụ đã commit bị thất bại nếu MongoDB audit tạm thời không sẵn sàng.

#### H. Hết hạn phiên giữa chừng

Nếu access token hết hạn ngay khi đang thao tác, request nhận `401/1019`. Client có
thể gọi refresh một lần, retry request gốc bằng access token mới; nếu refresh cũng
thất bại thì xóa phiên và đưa về đăng nhập. Không retry vô hạn vì có thể tạo thao
tác trùng hoặc che giấu lỗi quyền.

#### I. Cạnh tranh cập nhật và dữ liệu đã thay đổi

Hai admin có thể cùng sửa một đề tài, hội đồng hoặc lịch. Request đến sau phải
được kiểm tra lại trạng thái và quan hệ hiện tại (ví dụ đề tài vừa được gán nhóm,
hội đồng vừa bị tạm dừng). Nếu không còn đủ điều kiện, backend trả `409`; UI cần
refresh danh sách và yêu cầu người dùng chọn lại bản ghi thay vì ghi đè mù.

#### J. Quy tắc phản hồi thống nhất cho frontend

Mỗi response nên được xử lý theo thứ tự:

1. HTTP `2xx`: cập nhật dữ liệu từ `result`, đóng modal và hiển thị notification.
2. HTTP `400`: giữ modal, hiển thị lỗi nhập liệu/định dạng.
3. HTTP `401`: thử refresh một lần; thất bại thì đăng xuất cục bộ.
4. HTTP `403`: hiển thị không có quyền, không retry.
5. HTTP `404`: thông báo bản ghi không tồn tại và tải lại danh sách.
6. HTTP `409`: hiển thị xung đột/điều kiện nghiệp vụ và tải lại bản ghi liên quan.
7. HTTP `500`: giữ dữ liệu biểu mẫu, ghi log và cho phép thử lại an toàn.

## 3. Thiết lập dữ liệu đào tạo

### 3.1. Năm học

Năm học là bản ghi cha, phải tồn tại trước khi tạo đợt bảo vệ. API cho phép Admin
hoặc Faculty tạo/cập nhật/xóa; Admin, Faculty và Supervisor được xem dữ liệu.

#### Sơ đồ CRUD backend

```text
Admin/Faculty
   ├─ Tạo ───────→ POST /academic-year/create-academic-year
   ├─ Danh sách ─→ GET  /academic-year/get-all-academic-year?page&size
   ├─ Chi tiết ───→ GET  /academic-year/{academicId}
   ├─ Tìm theo tên → GET /academic-year/name/{academicYear}
   ├─ Sửa ───────→ PUT  /academic-year/{academicId}
   └─ Xóa ───────→ DELETE /academic-year/{academicId}
```

Request gồm `academicYear` và `description`. Backend trim mô tả, chuẩn hóa dấu
`/` hoặc `-` (kể cả có khoảng trắng) về `YYYY-YYYY`, sau đó kiểm tra năm kết thúc
lớn hơn năm bắt đầu. Khi hợp lệ, entity được lưu trong transaction và trả về
`ApiResponse<AcademicYearResponse>` với HTTP `200`.

#### Các nhánh xử lý

1. **Tạo:** kiểm tra định dạng và tên chưa tồn tại (không phân biệt hoa thường),
   rồi insert bản ghi mới.
2. **Xem:** repository tìm theo id/tên; kết quả được mapper sang DTO, không trả
   entity trực tiếp.
3. **Sửa:** tải bản ghi cũ, kiểm tra tên mới không trùng bản ghi khác, cập nhật
   description và lưu. Id trong URL quyết định bản ghi, không lấy id từ body.
4. **Xóa:** chỉ xóa vật lý khi năm học chưa có đợt bảo vệ. Nếu đã có quan hệ, giữ
   nguyên dữ liệu và trả lỗi xung đột để tránh làm hỏng các khóa ngoại.

#### Các trường hợp thất bại

| Điều kiện | Mã lỗi | HTTP | Kết quả |
| --- | ---: | :---: | --- |
| Bỏ trống tên năm học | `1047` | 400 | Không insert/update |
| Sai định dạng hoặc năm kết thúc không lớn hơn năm bắt đầu | `1048` | 400 | Không insert/update |
| Mô tả vượt giới hạn DTO | `1012` | 400 | Không ghi dữ liệu |
| Tên năm học đã tồn tại | `1049` | 409 | Không tạo bản ghi trùng |
| `academicId` không tồn tại/null | `1046` | 404 | Không xem/sửa/xóa |
| Đã có đợt bảo vệ hoặc dữ liệu hội đồng | `1050` | 409 | Không xóa năm học |
| Không đủ role gọi thao tác ghi | `1026` | 403 | Security chặn trước service |

Frontend chỉ thông báo thành công sau khi nhận `2xx`; khi lỗi `409` cần giữ modal và
hiển thị tên đã trùng, khi lỗi `404` cần tải lại danh sách vì bản ghi có thể đã bị
xóa bởi người dùng khác.

### 3.2. Đợt bảo vệ

Đợt bảo vệ thuộc một năm học và là phạm vi dữ liệu cho đề tài, milestone, ghi danh,
hội đồng và lịch bảo vệ. API đọc được mở cho Admin/Faculty/Supervisor/Student ở
một số endpoint; API tạo, sửa, xóa chỉ cho Admin hoặc Faculty.

#### Sơ đồ CRUD backend

```text
Admin/Faculty
   ├─ Tạo ───────────→ POST /defense-period/create-defense-period
   ├─ Danh sách ─────→ GET  /defense-period/get-all-defense-period
   ├─ Chi tiết ───────→ GET  /defense-period/{defensePeriodId}
   ├─ Theo năm học ──→ GET  /defense-period/academic-year/{academicId}
   ├─ Sửa ────────────→ PUT  /defense-period/{defensePeriodId}
   └─ Xóa ────────────→ DELETE /defense-period/{defensePeriodId}
```

Request gồm `periodName`, `startDate`, `endDate`, `projectType`, `status` và
`academicId`. Backend thực hiện theo thứ tự:

1. Kiểm tra tên, ngày bắt đầu/kết thúc, trạng thái và năm học không null.
2. Kiểm tra `endDate` không trước `startDate`.
3. Tìm `AcademicYearEntity`; năm học không tồn tại thì dừng trước khi insert.
4. Kiểm tra tên đợt không trùng trong cùng năm học (không phân biệt hoa thường).
5. Chuẩn hóa tên/loại đồ án, gắn quan hệ năm học và lưu trong transaction.

#### Vòng đời trạng thái

Các giá trị hợp lệ là `PENDING` (Chờ diễn ra), `ONGOING` (Đang diễn ra) và
`FINISHED` (Đã kết thúc). Luồng dự kiến:

```text
PENDING → ONGOING → FINISHED
    └──────────────→ FINISHED (khi ngày kết thúc đã qua)
```

Service có hàm `finishExpiredPeriods()` để cập nhật mọi đợt có `endDate` trước ngày
hiện tại sang `FINISHED`. Đây là hàm nghiệp vụ dùng cho scheduler/startup; cần bảo
đảm ứng dụng thực sự gọi hàm này thì trạng thái mới tự động cập nhật. API tạo hiện
nhận `status` từ request, vì vậy frontend phải gửi `PENDING` khi tạo đợt mới, không
để giá trị rỗng hoặc tự hiển thị `FINISHED`.

#### Cập nhật và xóa

- Sửa tải đợt theo id, kiểm tra năm học mới tồn tại và tên không trùng đợt khác
  trong cùng năm, sau đó cập nhật toàn bộ request.
- Xóa chỉ thực hiện khi đợt chưa có đề tài hoặc milestone. Các dữ liệu liên quan
  phải được xử lý trước; không xóa cưỡng bức để tránh mất lịch sử bảo vệ.
- Các service đề tài/milestone/lịch phải kiểm tra lại `FINISHED` và từ chối thao tác
  tạo mới khi đợt đã kết thúc, ngay cả khi UI vẫn còn hiển thị nút.

#### Các trường hợp thất bại

| Điều kiện | Mã lỗi | HTTP | Kết quả |
| --- | ---: | :---: | --- |
| Request null hoặc thiếu nhiều trường khi lọt tới service | `1053` | 400 | Không insert/update |
| Tên đợt bỏ trống | `1052` | 400 | Không ghi dữ liệu |
| Thiếu ngày bắt đầu/kết thúc | `1057`, `1058` | 400 | Không ghi dữ liệu |
| Thiếu trạng thái hoặc năm học trong request | `1011`, `1021` | 400 | Controller `@Valid` chặn trước service |
| Ngày kết thúc trước ngày bắt đầu | `1054` | 400 | Không ghi dữ liệu |
| Năm học không tồn tại | `1046` hoặc `1021` (id null) | 404/400 | Không tạo/cập nhật đợt |
| Tên đợt trùng trong cùng năm học | `1055` | 409 | Không tạo bản ghi trùng |
| Đợt đang chứa đề tài hoặc milestone | `1056` | 409 | Không xóa đợt |
| Đợt đã kết thúc nhưng vẫn tạo đề tài/mốc/lịch | `1082` hoặc lỗi nghiệp vụ tương ứng | 409 | Request bị rollback |
| Không đủ role ghi dữ liệu | `1026` | 403 | Security chặn trước service |

Khi đổi trạng thái từ `FINISHED` về `PENDING`/`ONGOING`, backend hiện nhận giá trị
status mới qua API cập nhật; các module phụ thuộc vẫn phải kiểm tra ngày và điều
kiện thực tế trước khi cho tạo dữ liệu. UI cần tải lại bản ghi sau khi đổi trạng
thái để tránh hiển thị badge cũ.

#### Lưu ý kiểm thử thực tế

- Trạng thái khi tạo là giá trị do request quyết định; service chưa tự suy ra trạng
  thái từ ngày bắt đầu. Nếu UI khởi tạo mặc định sai thành `FINISHED`, backend vẫn
  có thể lưu đúng giá trị sai đó.
- Hàm `finishExpiredPeriods()` tồn tại nhưng không nằm trong controller; cần kiểm
  tra scheduler/startup có gọi hàm hay không. Nếu chưa được gọi, đợt quá hạn sẽ
  không tự đổi badge cho tới khi có một luồng khác kích hoạt cập nhật.
- Service cập nhật đợt chưa tự chặn mọi thay đổi trên bản ghi `FINISHED`; các
  service tạo đề tài, milestone và lịch mới là nơi đang kiểm tra `DEFENSE_PERIOD_FINISHED`.
  Vì vậy UI không được coi `FINISHED` chỉ là nhãn hiển thị mà phải xử lý lỗi `409`
  từ backend.
- Xóa đợt hiện kiểm tra trực tiếp danh sách đề tài và milestone. Nếu database còn
  quan hệ phụ thuộc khác, lỗi ràng buộc có thể được handler chuyển thành `1072`;
  cần thử xóa trên dữ liệu có hội đồng, ghi danh và lịch trước khi triển khai.

### 3.3. Ngành học và lớp học

1. Tạo ngành với mã ngành, tên ngành và mô tả.
2. Tạo lớp với mã lớp, tên lớp, ngành học và mô tả.
3. Dùng ngành/lớp làm dữ liệu tham chiếu khi import hoặc tạo hồ sơ sinh viên.
4. Mã ngành, mã lớp phải duy nhất theo quy tắc backend.

## 4. Quản lý tài khoản

### 4.1. Quy tắc chung của CRUD tài khoản

Các thao tác quản trị tài khoản đều yêu cầu `ROLE_ADMIN`. Controller chuyển DTO cho
service; service chuẩn hóa chuỗi, kiểm tra ràng buộc, mã hóa mật khẩu bằng BCrypt,
tạo/cập nhật `UserEntity` cùng hồ sơ sinh viên hoặc giảng viên rồi mapper thành
`ApiResponse`.

```text
Form/Excel của Admin
   ↓
Controller (/register-student hoặc /register-lecture)
   ↓
@PreAuthorize ROLE_ADMIN + validate DTO
   ↓
Service chuẩn hóa dữ liệu và kiểm tra trùng
   ↓
UserRepository + StudentRepository/LectureRepository
   ↓
ApiResponse { code, message, result }
```

Mật khẩu khi tạo thủ công/import là giá trị nhận từ request hoặc cột `password` của
file, được lưu dưới dạng BCrypt; backend không lưu mật khẩu thô. Khi reset, backend
sinh mật khẩu tạm, mã hóa và trả mật khẩu tạm một lần trong response để Admin bàn
giao cho người dùng.

### 4.2. CRUD Sinh viên

#### Sơ đồ luồng

```text
Admin chọn thao tác
   ├─ Tạo ───────→ POST /register-student/create-user
   ├─ Xem list ──→ GET  /register-student/get-all-student?page&size&keyword
   ├─ Xem chi tiết → GET /register-student/{userName}
   ├─ Sửa ───────→ PATCH /register-student/{userId}
   ├─ Xóa ───────→ DELETE /register-student/username/{userName}
   ├─ Reset MK ──→ PATCH /register-student/reset-password/{userName}
   ├─ Import ────→ POST /register-student/import (multipart .xlsx)
   └─ Export ────→ GET /register-student/export?year
```

#### Tạo sinh viên

Request gồm `userName`, `password`, `studentCode`, `fullName`, `email`, `phone` và
`classCode`. Service dùng mã lớp để tra cứu `ClassEntity` trước khi liên kết hồ sơ
sinh viên; `classId` chỉ còn là fallback tương thích với client cũ.

1. Trim username/mã/họ tên, chuyển email và số điện thoại rỗng thành `null`.
2. Kiểm tra trường bắt buộc và lớp tồn tại; các ràng buộc định dạng của DTO chỉ
   được áp dụng đầy đủ ở những endpoint có `@Valid`.
3. Kiểm tra username, mã sinh viên, email và số điện thoại chưa được dùng.
4. Gán role mặc định `STUDENT`, trạng thái `ACTIVE`, mã hóa mật khẩu.
5. Lưu `UserEntity` và `StudentEntity` trong cùng transaction rồi trả hồ sơ.

#### Xem và tìm kiếm

Danh sách hỗ trợ phân trang và tìm theo tên/mã sinh viên. Chi tiết yêu cầu
`userName`; nếu không có hồ sơ sinh viên tương ứng, service trả `404` thay vì trả
user chung. Dữ liệu trả về đã được mapper, không chứa mật khẩu BCrypt.

#### Cập nhật

`PATCH /{userId}` cập nhật username, mã sinh viên, họ tên, email, số điện thoại và
lớp. Backend tải đúng user có hồ sơ sinh viên, kiểm tra trùng với bản ghi khác,
tra cứu lớp bằng `classCode`, sau đó cập nhật hai entity. Mật khẩu và role không đổi qua API
này; dùng reset mật khẩu hoặc module phân quyền riêng.

#### Xóa và reset mật khẩu

Xóa sinh viên hiện là **xóa mềm**: service đổi `UserEntity.status` thành `DELETED`
và giữ lại hồ sơ/quan hệ nhóm, đề tài, ghi danh và audit để bảo toàn lịch sử. Reset
mật khẩu sinh ra mật khẩu tạm ngẫu nhiên, chỉ trả bản rõ trong response hiện tại;
Admin cần yêu cầu sinh viên đổi lại sau khi đăng nhập.

#### Import/export

File `.xlsx` phải có đúng header:
`userName, password, studentCode, fullName, email, phone, classCode`. Backend đọc
từng dòng, kiểm tra trùng cả trong file và trong CSDL, rồi chạy transaction riêng
cho từng dòng. Vì vậy file có thể trả `successRows` và `failedRows` đồng thời; các
dòng thành công đã commit vẫn được giữ, còn dòng lỗi có số dòng và message để sửa
import lại. File cũ có cột `classId` vẫn được đọc tạm thời để tương thích, nhưng
template và luồng mới phải dùng `classCode`. Export nhận năm hợp lệ (2000–2100) và
trả file Excel.

#### Các trường hợp thất bại của sinh viên

| Điều kiện | Mã lỗi | HTTP | Backend không thực hiện |
| --- | ---: | :---: | --- |
| Thiếu username/mật khẩu/mã sinh viên/họ tên/mã lớp | `1028`, `1027`, `1036`, `1030`, `1037` | 400 | Không tạo/cập nhật hồ sơ |
| Username đã tồn tại | `1016` | 400 | Không tạo/cập nhật user |
| Mã sinh viên đã tồn tại | `1018` | 400 | Không tạo/cập nhật hồ sơ |
| Email hoặc số điện thoại đã tồn tại | `1017`, `1032` | 400/409 | Không ghi dữ liệu trùng |
| Lớp không tồn tại hoặc classCode sai | `1012` | 400 | Không tạo/cập nhật sinh viên |
| Không tìm thấy user/hồ sơ | `1021` | 404 | Không sửa/xóa/reset |
| File không phải `.xlsx`, sai header hoặc rỗng | `1034` | 400 | Không đọc/import file |
| Dữ liệu đã tồn tại trong file/CSDL | `1173` | 409 | Dừng bước kiểm tra trùng |

### 4.3. CRUD Giảng viên

#### Sơ đồ luồng

```text
Admin chọn thao tác
   ├─ Tạo ───────→ POST /register-lecture/create-user
   ├─ Xem list ──→ GET  /register-lecture/get-all-lecture?page&size&keyword
   ├─ Xem chi tiết → GET /register-lecture/username/{userName}
   ├─ Sửa ───────→ PATCH /register-lecture/{userId}
   ├─ Xóa ───────→ DELETE /register-lecture/{userId}
   ├─ Reset MK ──→ PATCH /register-lecture/reset-password/{userName}
   └─ Import ────→ POST /register-lecture/import (multipart .xlsx)
```

#### Tạo giảng viên

Request gồm `userName`, `password`, `lectureCode`, `fullName`, `degree`, `email` và
`phone`. Service chuẩn hóa dữ liệu, kiểm tra định dạng và trùng username/mã giảng
viên/email/số điện thoại, sau đó gán role mặc định `SUPERVISOR`, trạng thái `ACTIVE`,
lưu `UserEntity` và `LectureEntity` trong transaction.

#### Xem, tìm kiếm và cập nhật

Danh sách hỗ trợ phân trang/từ khóa theo tên hoặc mã giảng viên; chi tiết tìm theo
username. Cập nhật cho phép thay đổi các trường hồ sơ, mật khẩu (nếu gửi) và tập
role. Nếu request có `roles`, backend tải toàn bộ role theo id; chỉ cần một role
không tồn tại là hủy toàn bộ cập nhật. Mật khẩu mới luôn được BCrypt trước khi lưu.

#### Xóa và reset mật khẩu

Xóa giảng viên cũng là xóa mềm (`status=DELETED`), không xóa vật lý các phân công
hướng dẫn, phản biện, hội đồng hoặc lịch đã có để giữ lịch sử. Tài khoản bị xóa mềm
không thể đăng nhập và không nên được chọn cho phân công mới. Reset mật khẩu dùng
cùng cơ chế sinh mật khẩu tạm như sinh viên.

#### Import

Header bắt buộc:
`userName, password, lectureCode, fullName, degree, email, phone`. Backend đọc
từng dòng, kiểm tra trùng trong file/CSDL và tạo mỗi dòng trong transaction riêng;
response trả tổng số dòng, số dòng thành công, số dòng lỗi và chi tiết lỗi từng dòng.

#### Các trường hợp thất bại của giảng viên

| Điều kiện | Mã lỗi | HTTP | Backend không thực hiện |
| --- | ---: | :---: | --- |
| Thiếu/sai username, mật khẩu, mã, họ tên | `1028`, `1027`, `1029`, `1030` | 400 | Không tạo/cập nhật hồ sơ |
| Email hoặc số điện thoại sai định dạng | `1014`, `1031` | 400 | Không ghi dữ liệu |
| Username đã tồn tại | `1016` | 400 | Không tạo/cập nhật user |
| Mã giảng viên đã tồn tại | `1033` | 400/409 | Không tạo/cập nhật hồ sơ |
| Email hoặc số điện thoại đã tồn tại | `1017`, `1032` | 400/409 | Không ghi dữ liệu trùng |
| Không tìm thấy user/hồ sơ giảng viên | `1021` | 404 | Không sửa/xóa/reset |
| Role được gửi lên không tồn tại | `1009` | 409 | Hủy cập nhật toàn bộ |
| File không phải `.xlsx`, sai header/rỗng | `1034` | 400 | Không import |
| Dữ liệu import đã tồn tại | `1173` | 409 | Không tạo dòng trùng |

### 4.4. Lưu ý khi kiểm thử luồng tài khoản

- API tạo sinh viên hiện gọi service trực tiếp nhưng controller chưa gắn `@Valid`;
  service vẫn kiểm tra các trường bắt buộc, còn giới hạn độ dài/định dạng email và
  điện thoại của DTO có thể không được áp dụng đầy đủ như API giảng viên. Đây là
  điểm cần kiểm thử riêng nếu frontend cho phép nhập dữ liệu tự do.
- API cập nhật sinh viên yêu cầu `classCode` trong service; thiếu hoặc không tìm thấy
  mã lớp sẽ nhận lỗi dữ liệu lớp và rollback.
- Xóa sinh viên/giảng viên là xóa mềm, nên bản ghi vẫn xuất hiện trong quan hệ
  lịch sử nhưng tài khoản không còn đăng nhập được do `ACCOUNT_INACTIVE`.
- Import dùng transaction riêng cho từng dòng. Vì thế kết quả một phần là hợp lệ:
  dòng thành công được commit, dòng thất bại được trả trong danh sách lỗi; khi
  import lại cần loại bỏ các dòng đã thành công để tránh `1173`.
- Service xóa giảng viên hiện tra cứu theo `userId`; tầng UI phải chỉ gửi id của
  user có hồ sơ giảng viên để tránh đánh dấu nhầm tài khoản loại khác. Đây là điều
  kiện nên bổ sung kiểm thử hồi quy khi thay đổi API.

### 4.5. Phân quyền người dùng

Module **Phân quyền** dùng để gán **role cho user**, không dùng để chỉnh từng permission trực tiếp trên user.

1. Tải danh sách người dùng và role.
2. Chọn một hoặc nhiều role cho user.
3. Lưu danh sách role mới bằng chức năng cập nhật role.
4. Permission được kế thừa từ role đã cấu hình.

Việc tạo/cập nhật/xóa role hoặc permission là cấu hình hệ thống; chỉ thực hiện khi có yêu cầu quản trị và phải đảm bảo không xóa role đang được sử dụng.

## 5. Quản lý nhóm sinh viên

Admin tạo nhóm, sau đó thêm sinh viên bằng cách chọn từng người, chọn nhiều người hoặc import Excel.

- Một sinh viên không được thuộc nhiều nhóm cùng lúc.
- Có thể xem danh sách thành viên và nhóm mà sinh viên hiện đang thuộc.
- Có thể xóa sinh viên khỏi nhóm; backend phải đồng bộ quan hệ nhóm–sinh viên.
- Có thể cập nhật tên, mô tả, ngày thành lập và đề tài của nhóm.
- Một nhóm chỉ được gán một đề tài; một đề tài không được gán cho nhiều nhóm.
- Khi nhóm chọn đề tài đã được phê duyệt, các đăng ký đề tài đang chờ của nhóm sẽ được hủy theo nghiệp vụ.

## 6. Quản lý đề tài và đăng ký đề tài

### 6.1. Phân biệt hai nghiệp vụ

Module **Quản lý đề tài** quản lý bản ghi `TopicEntity`; module **Duyệt đăng ký đề tài**
quản lý bản ghi `TopicRegistrationEntity`. Hai bản ghi có liên hệ nhưng không thay
thế cho nhau:

| Luồng | Người khởi tạo | Bản ghi tạo ra | Khi nào nhóm được gán |
| --- | --- | --- | --- |
| Tạo đề tài quản trị | Admin, Faculty, Supervisor | `TopicEntity` | Có thể chưa có nhóm lúc tạo; gán ở nghiệp vụ sau |
| Sinh viên đề xuất đề tài mới | Student | `TopicEntity` với `categoryTopic=STUDENT` | Khi Admin/Faculty duyệt đề xuất |
| Sinh viên chọn đề tài có sẵn | Student | `TopicRegistrationEntity` | Khi Admin/Faculty duyệt đăng ký |

Đề tài do Admin/Faculty tạo không bắt buộc có sinh viên ngay vì đây là kho đề tài
được chuẩn bị trước cho một đợt bảo vệ. Tuy nhiên đề tài chưa có nhóm không được
phân công hướng dẫn/phản biện và không đủ điều kiện lập lịch bảo vệ. Các nghiệp vụ
downstream phải kiểm tra lại điều kiện này ở backend, không chỉ ẩn nút trên UI.

### 6.2. Quản lý `TopicEntity` (CRUD và duyệt đề tài)

#### Sơ đồ request tổng quát

```text
Frontend
  │ JWT + JSON/multipart
  ▼
TopicControler (/topics)
  │ @PreAuthorize + @Valid + ánh xạ lỗi
  ▼
TopicService (@Transactional)
  │ kiểm tra user/nhóm/đợt bảo vệ/trạng thái/trùng tên
  ├─ TopicRepository
  ├─ DefensePeriodRepository
  ├─ TeamRepository
  └─ UserRepository + TopicMapper
  ▼
ApiResponse<TopicResponse> / lỗi HTTP
```

#### API và xử lý thành công

| Thao tác | API | Quyền | Xử lý backend và kết quả |
| --- | --- | --- | --- |
| Tạo | `POST /topics` | Admin/Faculty/Supervisor/Student (Student bị giới hạn nghiệp vụ) | Kiểm tra request, đợt bảo vệ còn hoạt động, tên không trùng trong đợt; chuẩn hóa chuỗi; gắn `createdBy`, `defensePeriod`, có thể gắn `proposedTeam`; lưu `DRAFT`; trả `TopicResponse`. |
| Import | `POST /topics/import` (`multipart/form-data`, part `file`) | Như API tạo | Chỉ nhận `.xlsx`; kiểm tra header `title, description, objective, technology, categoryTopic, defensePeriodId`; xử lý từng dòng và trả tổng số thành công/thất bại cùng lỗi theo dòng. |
| Chi tiết | `GET /topics/{topicId}` | Đã đăng nhập | Tải đề tài và các quan hệ hiển thị, mapper sang DTO; id không tồn tại trả 404. |
| Danh sách | `GET /topics?page&size&academicYearId&defensePeriodId&categoryTopic&status&keyword&excludeStudentProposals` | Đã đăng nhập | Lọc theo đợt/năm/nguồn/trạng thái/từ khóa; `excludeStudentProposals=true` loại đề xuất sinh viên đang chờ, nháp hoặc bị từ chối khỏi màn quản lý chung. |
| Đề xuất của tôi | `GET /topics/my-proposals` | Student | Lấy nhóm của tài khoản hiện tại và các đề tài nguồn Student của nhóm. Không có nhóm trả lỗi; response bổ sung tên/mã nhóm để UI không hiển thị thiếu nhóm. |
| Cập nhật | `PATCH /topics/{topicId}` | Chủ sở hữu hoặc Admin/Faculty | Chỉ `DRAFT`/`REJECTED` được sửa; kiểm tra lại đợt bảo vệ, tên trùng và quyền; cập nhật trường request trong transaction. |
| Xóa | `DELETE /topics/{topicId}` | Chủ sở hữu hoặc Admin/Faculty | Chỉ xóa khi chưa gán nhóm, hướng dẫn, phản biện hoặc lịch bảo vệ và đề tài còn trạng thái cho phép sửa. |
| Gửi duyệt | `POST /topics/{topicId}/submit-for-approval` | Chủ sở hữu hoặc Admin/Faculty | Kiểm tra đợt chưa kết thúc, chuyển `DRAFT`/`REJECTED` sang `PENDING_APPROVAL`, xóa lý do từ chối cũ. |
| Duyệt | `POST /topics/{topicId}/approve` | Admin/Faculty | Chấp nhận `DRAFT` (đề tài quản trị) hoặc `PENDING_APPROVAL`; đề tài Student được gán vào `proposedTeam` và cập nhật quan hệ hai chiều Team–Topic; chuyển `APPROVED`. |
| Từ chối | `POST /topics/{topicId}/reject` với `{ "reason": "..." }` | Admin/Faculty | Chỉ nhận `PENDING_APPROVAL`, bắt buộc lý do, chuyển `REJECTED` và lưu lý do để sinh viên chỉnh sửa/gửi lại. |

Các API ghi được thực hiện trong transaction. Với duyệt đề tài Student, việc cập
nhật `team.topic`, `topic.team` và trạng thái phải cùng commit; nếu một bước thất
bại toàn bộ thay đổi rollback, tránh tình trạng giao diện báo duyệt nhưng nhóm vẫn
không có đề tài.

#### Điều kiện và lỗi CRUD

| Điều kiện thất bại | Mã lỗi | HTTP | Ý nghĩa/UI cần xử lý |
| --- | ---: | :---: | --- |
| Chưa đăng nhập hoặc hết phiên | `1000`/`1019` | 401 | Refresh token một lần; thất bại thì đưa về đăng nhập. |
| Không có role hoặc không phải chủ sở hữu | `1026` | 403 | Giữ modal, hiển thị không có quyền; không retry. |
| Tên trống | `1076` | 400 | Báo ngay trên trường tên. |
| Nguồn đề xuất trống/sai enum `LECTURER`/`STUDENT` | `1077` | 400 | Yêu cầu chọn nguồn hợp lệ. |
| Đợt bảo vệ không tồn tại | `1051` | 404 | Tải lại danh sách đợt và yêu cầu chọn lại. |
| Đợt bảo vệ đã kết thúc | `1082` | 409 | Không cho tạo/sửa/gửi duyệt; giữ dữ liệu form. |
| Tên trùng trong cùng đợt | `1078` | 409 | Hiển thị đề tài trùng, không insert/update. |
| Đề tài không tồn tại | `1074` | 404 | Đóng modal và refresh danh sách. |
| Sửa/gửi/duyệt/từ chối sai trạng thái | `1080` | 409 | Refresh bản ghi vì dữ liệu có thể đã đổi bởi người khác. |
| Đề tài đang được nhóm/giảng viên/lịch dùng | `1079` | 409 | Không xóa; giải thích quan hệ đang dùng. |
| Duyệt Student nhưng nhóm đã có đề tài hoặc đề tài đã gán nhóm khác | `1084`/`1045` | 409 | Không ghi đè nhóm; tải lại đề tài và nhóm. |
| Từ chối không có lý do | `1081` | 400 | Không gửi request cho đến khi nhập lý do. |
| File import rỗng/sai phần mở rộng/header | `1034` | 400 | Hiển thị yêu cầu mẫu Excel đúng cột. |
| Một dòng Excel sai dữ liệu hoặc trùng | `1035`/`1078`/`1173` | 400/409 | Hiển thị số dòng, tiêu đề và lỗi; không báo thành công toàn bộ file. |

### 6.3. Sinh viên đề xuất đề tài mới

```text
Student đăng nhập
   │ POST /topics (categoryTopic=STUDENT)
   ▼
TopicService
   ├─ tìm Team theo user hiện tại
   ├─ bắt buộc Team tồn tại và chưa có Topic
   ├─ chặn đề xuất DRAFT/PENDING khác của cùng Team
   ├─ kiểm tra defense period còn hoạt động + tên không trùng
   └─ tạo Topic(DRAFT, proposedTeam=Team, createdBy=user)
   │
   └─ POST /topics/{id}/submit-for-approval
          ▼
      Topic(PENDING_APPROVAL)
          │ Admin/Faculty xem tại Duyệt đăng ký đề tài
          ├─ POST /topics/{id}/approve → gán Team + APPROVED
          └─ POST /topics/{id}/reject    → REJECTED + reason
```

Điều kiện bắt buộc là sinh viên phải thuộc nhóm và nhóm chưa có đề tài. Chỉ một
thành viên trong nhóm được tạo đề xuất đang hoạt động; sau khi một đề xuất ở
`DRAFT`/`PENDING_APPROVAL` hoặc nhóm đã có đề tài, các thành viên còn lại đều bị
chặn. Khi duyệt, backend dùng `proposedTeam` (không suy đoán lại từ người gửi nếu
đã có quan hệ), gán Team–Topic hai chiều và lưu trong cùng transaction.

Đề xuất bị từ chối vẫn là `TopicEntity` để giữ lịch sử. Sinh viên có thể sửa và
gửi lại khi trạng thái là `REJECTED`, miễn đợt bảo vệ chưa kết thúc và nhóm vẫn
chưa có đề tài. UI nên hiển thị đề xuất này ở **Duyệt đăng ký đề tài**; màn quản lý
đề tài có thể dùng `excludeStudentProposals=true` để tránh hiển thị trùng.

### 6.4. Sinh viên đăng ký đề tài đã được duyệt (`TopicRegistrationEntity`)

#### Sơ đồ request

```text
Student
  │ POST /topic-registrations {topicId, priority, preferredSupervisorId, note}
  ▼
TopicRegistrationService (@Transactional)
  ├─ currentStudent + Team của user
  ├─ Team chưa có Topic
  ├─ Topic = APPROVED, chưa có Team và chưa bị nhóm khác dùng
  ├─ tìm GraduationEnrollment đúng sinh viên + defensePeriod của Topic
  ├─ chặn đăng ký PENDING trùng nhóm/đợt
  └─ lưu Registration(PENDING, team, topic, enrollment)
          │
          ├─ GET /topic-registrations/my              (Student)
          ├─ GET /topic-registrations?status=...      (Admin/Faculty)
          ├─ PATCH /topic-registrations/{id}/approve
          │      └─ gán Topic cho Team, APPROVED; hủy PENDING khác của nhóm
          ├─ PATCH /topic-registrations/{id}/reject {reason}
          │      └─ REJECTED + reviewedBy/reviewedAt/reason
          └─ PATCH /topic-registrations/{id}/cancel (Student, chỉ PENDING)
```

`priority` mặc định là 1 nếu không gửi hoặc nhỏ hơn 1; `preferredSupervisorId`
không bắt buộc nhưng nếu gửi phải trỏ tới hồ sơ giảng viên tồn tại. Đăng ký phải
tham chiếu đúng hồ sơ ghi danh của sinh viên trong cùng đợt bảo vệ; nếu không có
hồ sơ tương ứng, backend từ chối thay vì tạo đăng ký mồ côi.

Khi duyệt, service kiểm tra lại toàn bộ điều kiện ngay trước khi ghi vì đề tài có
thể vừa được người khác chọn. Sau đó cập nhật `team.topic`, `topic.team`, trạng
thái đăng ký `APPROVED`; các đăng ký `PENDING` khác của cùng nhóm được chuyển
`CANCELLED` và ghi lý do “Nhóm đã được duyệt một đề tài khác”. Từ chối phải có lý
do; hủy chỉ do chính sinh viên sở hữu và chỉ áp dụng cho `PENDING`.

#### Các trường hợp thất bại

| Điều kiện | Mã lỗi | HTTP | Kết quả |
| --- | ---: | :---: | --- |
| Tài khoản không phải Student | `1026` | 403 | Không cho gọi API đăng ký. |
| Student chưa có hồ sơ hoặc chưa thuộc nhóm | `1106`/`1178` | 404/409 | Yêu cầu tạo/tham gia nhóm trước. |
| Nhóm đã có đề tài | `1084` | 409 | Ẩn nút đăng ký; không tạo bản ghi mới. |
| Đề tài không tồn tại | `1074` | 404 | Refresh danh sách đề tài. |
| Đề tài chưa `APPROVED`, đã có nhóm hoặc bị chọn trước | `1083` | 409 | Thông báo đề tài không còn khả dụng. |
| Không có `GraduationEnrollment` đúng sinh viên/đợt | `1105` | 409 | Không tạo đăng ký thiếu liên kết đợt. |
| Nhóm đã có đăng ký `PENDING` | `1175` | 409 | Hiển thị đăng ký hiện tại; không tạo trùng. |
| Đăng ký không tồn tại | `1174` | 404 | Đóng modal, tải lại danh sách duyệt. |
| Duyệt/từ chối/hủy bản ghi không ở `PENDING` | `1176` | 409 | Không thay đổi trạng thái đã chốt. |
| Từ chối thiếu lý do | `1177` | 400 | Giữ modal và yêu cầu nhập lý do. |
| Giảng viên ưu tiên không tồn tại | `1107` | 404 | Xóa lựa chọn sai và chọn lại. |

### 6.5. Vòng đời trạng thái và điều kiện liên module

```text
Topic (đề tài)
  DRAFT ──submit──→ PENDING_APPROVAL ──approve──→ APPROVED
    ▲                     │                          │
    └── sửa/gửi lại ← REJECTED                        └─ gán Team → đủ điều kiện downstream

TopicRegistration (đăng ký đề tài có sẵn)
  PENDING ──approve──→ APPROVED
     │                    └─ gán Topic cho Team
     ├─reject──→ REJECTED
     └─cancel──→ CANCELLED
```

`REGISTERED`, `IN_PROGRESS`, `COMPLETED` là các giá trị có trong enum đề tài nhưng
không được tự ý gán khi tạo/duyệt hai luồng trên; trạng thái phải phản ánh nghiệp
vụ thật và do API tương ứng cập nhật. Một đề tài chỉ được đưa sang phân công hướng
dẫn, phân công phản biện, hội đồng hoặc lịch bảo vệ khi tối thiểu:

1. `Topic.status=APPROVED`;
2. `Topic.team`/`Team.topic` đã tồn tại và nhất quán;
3. đợt bảo vệ chưa kết thúc;
4. với lịch bảo vệ, đề tài đã có giảng viên hướng dẫn đang hoạt động (nếu backend
   trả `1160` thì UI phải nêu rõ điều kiện còn thiếu).

Frontend phải lấy trạng thái và quan hệ nhóm từ response mới nhất sau mỗi thao tác,
không tự đổi badge trước khi backend trả `2xx`. Khi nhận `409`, UI tải lại đề tài,
nhóm và đăng ký liên quan để phản ánh cạnh tranh cập nhật; khi nhận `500`, giữ dữ
liệu form và cho phép thử lại an toàn.

## 7. Phân công giảng viên

### 7.1. Giảng viên hướng dẫn

Luồng này tạo `TopicSuperVisorEntity`, nối một đề tài với một giảng viên hướng
dẫn. Chỉ Admin/Faculty được tạo, sửa hoặc ngừng phân công.

#### Sơ đồ backend

```text
Admin/Faculty
   │ POST /topics/{topicId}/supervisors
   ▼
TopicSupervisorControler
   ▼
TopicSupervisorService (@Transactional)
   ├─ tìm đề tài
   ├─ kiểm tra trạng thái APPROVED/REGISTERED/IN_PROGRESS
   ├─ kiểm tra đợt bảo vệ chưa FINISHED
   ├─ tìm giảng viên theo lectureId hoặc lectureCode
   ├─ kiểm tra tài khoản giảng viên ACTIVE
   ├─ không trùng phân công và không trùng PRIMARY
   ├─ kiểm tra giới hạn số đề tài/giảng viên/đợt
   └─ lưu ACTIVE + assignedBy + assignedAt
```

API chính:

| Thao tác | API | Kết quả |
| --- | --- | --- |
| Danh sách tất cả | `GET /topic-supervisors?page&size` | Admin/Faculty xem phân công phân trang. |
| Gán hướng dẫn | `POST /topics/{topicId}/supervisors` | Body gồm `lectureId`, `role`, `note`; tạo phân công `PRIMARY`, `ACTIVE`. |
| Theo đề tài | `GET /topics/{topicId}/supervisors?page&size` | Xem lịch sử/phân công của đề tài. |
| Theo giảng viên | `GET /lecturers/{lectureId}/supervised-topics?page&size` | Admin/Faculty xem đề tài đang được giảng viên hướng dẫn. |
| Giảng viên hiện tại | `GET /topic-supervisors/me?page&size` | Admin/Faculty/Supervisor xem phân công của tài khoản hiện tại. |
| Cập nhật | `PATCH /topic-supervisors/{assignmentId}` | Chỉ cập nhật vai trò/ghi chú khi phân công còn `ACTIVE`. |
| Ngừng phân công | `PATCH /topic-supervisors/{assignmentId}/deactivate` | Bắt buộc lý do; chuyển `INACTIVE`, ghi `endedAt` và nối lý do vào ghi chú. |

Điều kiện nghiệp vụ cần giữ:

- Đề tài phải được duyệt và thuộc đợt bảo vệ chưa kết thúc.
- Giảng viên phải tồn tại, có tài khoản và tài khoản không ở `INACTIVE`/`DELETED`.
- Một đề tài chỉ có một `PRIMARY` đang hoạt động.
- Không tạo cùng một phân công ACTIVE cho cùng giảng viên và đề tài.
- Tổng số đề tài ACTIVE của giảng viên trong đợt không vượt cấu hình
  `app.supervision.max-topics-per-period` (mặc định 5).
- Khi thay giảng viên, nên ngừng bản ghi cũ rồi tạo bản ghi mới để giữ lịch sử,
  không xóa vật lý.

**Lưu ý về vai trò trong code hiện tại:** request nhận trường `role`, nhưng
`TopicSupervisorService.assign` hiện luôn tạo phân công với vai trò `PRIMARY` và
luôn kiểm tra giới hạn hướng dẫn chính. Nếu cần hỗ trợ `CO_SUPERVISOR`, service
phải sử dụng giá trị `role` từ request và áp dụng quy tắc riêng cho từng vai trò.

**Lưu ý kiểm tra hiện tại:** `TopicSupervisorService` đã kiểm tra trạng thái đề
tài, đợt bảo vệ, giảng viên và giới hạn tải, nhưng hàm
`findApprovedAssignableTopic` chưa kiểm tra `topic.team != null`. Nếu yêu cầu
“đề tài chưa có nhóm thì không được gán giảng viên” là bắt buộc, cần bổ sung kiểm
tra này ở backend, không chỉ khóa lựa chọn trên frontend.

Các lỗi thường gặp: `1126` không tìm thấy phân công, `1128` đã gán giảng viên cho
đề tài, `1129` đã có hướng dẫn chính, `1130` phân công không còn ACTIVE, `1131`
thiếu lý do ngừng, `1132` tài khoản giảng viên không hoạt động, `1133` vượt giới
hạn tải, `1074` không tìm thấy đề tài, `1083` đề tài chưa đủ điều kiện và `1082`
đợt bảo vệ đã kết thúc.

### 7.2. Giảng viên phản biện

Admin mở **Phân công phản biện đề tài**, chọn đề tài đã có nhóm và giảng viên phản biện, nhập hạn phản biện và ghi chú.

- Không chọn đề tài chưa được phê duyệt hoặc chưa có nhóm.
- Theo dõi các trạng thái `ASSIGNED`, `IN_PROGRESS`, `SUBMITTED`, `APPROVED`, `REVISION_REQUIRED`, `CANCELLED`.
- Giảng viên phản biện bắt đầu, gửi kết quả, yêu cầu chỉnh sửa hoặc hủy theo API backend.
- Admin theo dõi danh sách tổng hợp; giảng viên chỉ xem danh sách được phân công cho mình.

## 8. Milestone, bài nộp và nhận xét

### 8.1. Milestone

Milestone (`MilesStoneEntity`) là mốc công việc của một đợt bảo vệ, quy định thời
gian nhận bài, loại tài liệu và quy tắc nộp muộn. Admin hoặc Faculty quản lý
milestone; sinh viên chỉ nhìn thấy các mốc đang mở hoặc đã đóng.

#### Sơ đồ backend

```text
Admin/Faculty
   │ POST /defense-periods/{defensePeriodId}/milestones
   ▼
MilestoneControler → MilestoneService (@Transactional)
   ├─ tìm đợt bảo vệ và từ chối đợt đã FINISHED
   ├─ kiểm tra tên, loại, thời gian bắt đầu/hạn nộp
   ├─ kiểm tra toàn bộ khoảng thời gian nằm trong đợt bảo vệ
   ├─ kiểm tra không trùng tên trong cùng đợt
   ├─ chuẩn hóa danh sách phần mở rộng và giá trị mặc định
   └─ lưu DRAFT, sau đó Admin mở mốc để nhận bài
```

API và quyền truy cập:

| Thao tác | API | Quyền/kết quả |
| --- | --- | --- |
| Tạo | `POST /defense-periods/{defensePeriodId}/milestones` | Admin/Faculty; tạo `DRAFT`. |
| Danh sách theo đợt | `GET /defense-periods/{defensePeriodId}/milestones?page&size&status&type&keyword` | Người dùng đã đăng nhập; Student chỉ được lọc `OPEN`/`CLOSED`. |
| Danh sách tổng hợp | `GET /milestones?page&size&defensePeriodId&status&type&keyword` | Lọc theo đợt, trạng thái, loại và từ khóa. |
| Chi tiết | `GET /milestones/{milestoneId}` | Trả chi tiết nếu milestone hiển thị với vai trò hiện tại. |
| Sửa | `PUT /milestones/{milestoneId}` | Admin/Faculty; chỉ sửa khi `DRAFT`. |
| Mở nhận bài | `PATCH /milestones/{milestoneId}/open` | `DRAFT → OPEN`; đợt phải còn hoạt động. |
| Đóng nhận bài | `PATCH /milestones/{milestoneId}/close` | `OPEN → CLOSED`. |
| Hủy | `PATCH /milestones/{milestoneId}/cancel` | `DRAFT` hoặc `OPEN → CANCELLED`. |
| Xóa | `DELETE /milestones/{milestoneId}` | Chỉ xóa `DRAFT` chưa có bài nộp. |

Các trường cần nhập gồm tên, mô tả, loại mốc (`TOPIC_REGISTRATION_FORM`,
`ASSIGNMENT_FORM`, `PROJECT_DIARY`, `PROGRESS_REPORT`, `FINAL_REPORT`,
`DEFENSE_SLIDE`, `SOURCE_CODE`, `OTHER`), `startAt`, `deadline`,
`allowLateSubmission`, `required`, `maxFileSize` và `allowedFileTypes`.
Thời gian bắt đầu phải trước hạn nộp; cả hai phải nằm trong thời gian đợt bảo vệ;
tên không được trùng trong cùng đợt.

Trạng thái:

```text
DRAFT → OPEN → CLOSED
  └────────────→ CANCELLED
```

`DRAFT` có thể sửa/xóa; `OPEN` nhận bài; `CLOSED` và `CANCELLED` không nhận bài
mới. Các lỗi chính: `1082` đợt bảo vệ đã kết thúc, `1085` không tìm thấy mốc,
`1090` thời gian không hợp lệ, `1091` nằm ngoài đợt, `1092` trùng tên,
`1093` thao tác sai trạng thái, `1094` mốc đã có bài nộp và `1095` dung lượng tệp
tối đa không hợp lệ.

### 8.2. Bài nộp

#### Sơ đồ backend

```text
Student chọn file
   │ POST /milestones/{milestoneId}/submissions (multipart: file, teamId, note)
   ▼
SubmissionControler → SubmissionService (@Transactional)
   ├─ kiểm tra file tồn tại, sinh viên hiện tại và thành viên của nhóm
   ├─ kiểm tra nhóm/đợt bảo vệ khớp với milestone
   ├─ kiểm tra milestone OPEN và đã đến startAt
   ├─ nếu quá deadline: chỉ nhận khi allowLateSubmission = true
   ├─ kiểm tra phần mở rộng, loại bị chặn và maxFileSize
   ├─ từ chối nếu phiên bản mới nhất đã APPROVED
   ├─ lưu file an toàn, đăng ký dọn file khi rollback
   └─ tạo phiên bản kế tiếp với SUBMITTED và isLate
```

API bài nộp:

| Thao tác | API | Mục đích |
| --- | --- | --- |
| Nộp phiên bản | `POST /milestones/{milestoneId}/submissions` | Student gửi file và ghi chú cho nhóm. |
| Danh sách | `GET /submissions?teamId&milestoneId&status&late&page&size` | Lọc bài nộp; dữ liệu được giới hạn theo nhóm/giảng viên hướng dẫn. |
| Theo nhóm | `GET /teams/{teamId}/submissions?...` | Xem các bài của nhóm. |
| Lịch sử phiên bản | `GET /teams/{teamId}/milestones/{milestoneId}/submissions` | Sắp xếp phiên bản mới nhất trước. |
| Chi tiết/tải file | `GET /submissions/{submissionId}`, `GET /submissions/{submissionId}/file` | Xem metadata hoặc tải file khi có quyền. |
| Rút bài | `DELETE /submissions/{submissionId}` | Student chỉ rút phiên bản mới nhất đang `SUBMITTED`. |
| Bắt đầu review | `PATCH /submissions/{submissionId}/review` | Admin/Faculty/giảng viên hướng dẫn chuyển `SUBMITTED → UNDER_REVIEW`. |
| Yêu cầu sửa | `PATCH /submissions/{submissionId}/request-revision` | Ghi nhận comment bắt buộc và chuyển `REVISION_REQUIRED`. |
| Phê duyệt | `PATCH /submissions/{submissionId}/approve` | Ghi comment nghiệp vụ và chuyển `APPROVED`. |
| Từ chối | `PATCH /submissions/{submissionId}/reject` | Ghi comment nghiệp vụ và chuyển `REJECTED`. |

Sinh viên chỉ được nộp khi thuộc nhóm, nhóm có quan hệ đúng đợt bảo vệ (qua đề
tài hoặc ghi danh), milestone đang `OPEN` và đã đến giờ bắt đầu. Tệp rỗng, không
có phần mở rộng, thuộc nhóm bị chặn (`exe`, `sh`, `bat`, `cmd`, `jar`, `php`,
`js`, `html`, `htm`), vượt dung lượng hoặc không nằm trong
`allowedFileTypes` đều bị từ chối. Phần mở rộng được chuẩn hóa về chữ thường và
bỏ dấu chấm trước khi so sánh, vì vậy `PDF`, `.pdf` và `pdf` được đối chiếu cùng
cách.

Nếu quá hạn mà `allowLateSubmission = true`, backend vẫn nhận bài và đặt
`isLate = true`; nếu tắt tùy chọn này thì trả lỗi. Sau khi bài mới nhất được
`APPROVED`, mọi phiên bản mới cho cùng milestone bị chặn. Khi bị
`REVISION_REQUIRED` hoặc `REJECTED`, nhóm có thể nộp phiên bản kế tiếp nếu mốc
vẫn còn mở. File được lưu theo đợt/nhóm/mốc; nếu transaction thất bại, file vừa
ghi sẽ được xóa để không tạo file mồ côi.

Trạng thái bài nộp:

```text
SUBMITTED → UNDER_REVIEW ──→ APPROVED
       │             ├──────→ REJECTED
       │             └──────→ REVISION_REQUIRED → (nộp phiên bản mới)
       └────────────────────→ WITHDRAWN (chỉ bản mới nhất, trước review)
```

Lỗi cần hiển thị tiếng Việt ở frontend: `1096` không tìm thấy bài nộp, `1097`
thiếu file, `1098` file quá lớn, `1099` định dạng không được phép, `1100` mốc
không nhận bài, `1101` chưa đến giờ, `1102` hết hạn không cho nộp muộn, `1103`
thao tác sai trạng thái, `1104` bài mới nhất đã duyệt, `1105` nhóm không thuộc
đợt của mốc và `1109` lỗi lưu/đọc file.

### 8.3. Nhận xét

Nhận xét (`CommentEntity`) luôn gắn với một bài nộp và có loại `COMMENT`,
`REVISION_REQUEST`, `APPROVAL` hoặc `REJECTION`.

```text
Admin/Faculty/Supervisor thêm nhận xét thường
   │ POST /submissions/{submissionId}/comments
   ▼
CommentService → kiểm tra bài nộp + quyền review → lưu COMMENT

Review bài nộp → createWorkflowComment(...)
   ├─ APPROVAL khi phê duyệt
   ├─ REJECTION khi từ chối
   └─ REVISION_REQUEST khi yêu cầu chỉnh sửa
```

API và quy tắc:

| Thao tác | API | Quy tắc |
| --- | --- | --- |
| Thêm nhận xét | `POST /submissions/{submissionId}/comments` | Admin/Faculty/Supervisor có quyền review bài nộp. Nội dung bắt buộc. |
| Xem | `GET /submissions/{submissionId}/comments?page&size` | Admin/Faculty, giảng viên hướng dẫn và thành viên nhóm được xem theo quyền bài nộp. |
| Sửa | `PUT /submission-comments/{commentId}` | Chỉ nhận xét loại `COMMENT`; tác giả hoặc Admin được sửa, đánh dấu `edited`. |
| Xóa | `DELETE /submission-comments/{commentId}` | Chỉ nhận xét loại `COMMENT`; tác giả hoặc Admin được xóa mềm. |

Nhận xét gắn với thao tác review phải được gửi trong body `comment` và không được
để trống; không thể sửa/xóa các nhận xét hệ thống về phê duyệt, từ chối hoặc yêu
cầu chỉnh sửa. `1110` là không tìm thấy nhận xét, `1111` là thao tác không hợp
lệ với loại/trạng thái nhận xét, còn `1108` là nội dung nhận xét rỗng.

### 8.4. Tích hợp giao diện

- Trang quản trị milestone gọi danh sách theo `defensePeriodId`, hiển thị badge
  `DRAFT`, `OPEN`, `CLOSED`, `CANCELLED` và chỉ hiện nút phù hợp với trạng thái.
  Sau khi tạo/mở/đóng/hủy/xóa, frontend tải lại dữ liệu từ API thay vì tự đoán
  trạng thái.
- Trang sinh viên tải các milestone `OPEN`/`CLOSED`, hiển thị hạn nộp và cảnh báo
  nộp muộn. Nút nộp bị khóa khi backend trả `1100`, `1101`, `1102` hoặc khi phiên
  bản mới nhất đã được duyệt (`1104`); lịch sử phiên bản lấy từ API lịch sử và
  hiển thị cờ “Nộp muộn” theo `isLate`.
- Trang review của Admin/Faculty/giảng viên hướng dẫn tải danh sách bài nộp,
  cho phép bắt đầu review, phê duyệt, từ chối hoặc yêu cầu chỉnh sửa. Hai thao tác
  cuối phải yêu cầu nội dung nhận xét; sau mỗi thao tác phải tải lại bài nộp và
  danh sách comment.
- Chi tiết bài nộp hiển thị metadata, nút tải file và comment phân trang. Chỉ
  nhận xét loại `COMMENT` mới có nút sửa/xóa; nhận xét workflow chỉ đọc. Frontend
  dùng thông báo tiếng Việt theo `ErrorCode`, giữ nguyên dữ liệu form khi request
  thất bại để người dùng sửa và gửi lại.

## 9. Ghi danh đồ án vào đợt bảo vệ

Module **Ghi danh đồ án** dùng để ghi danh một hoặc nhiều sinh viên vào đợt bảo vệ.

1. Chọn đợt bảo vệ chưa kết thúc.
2. Chọn danh sách sinh viên hoặc ghi danh từng sinh viên.
3. Kiểm tra sinh viên chưa có bản ghi ghi danh trùng trong cùng đợt.
4. Lưu ghi danh với trạng thái ban đầu `ENROLLED`.
5. Admin có thể xem danh sách, lọc theo đợt/trạng thái và cập nhật trạng thái theo các trạng thái backend hợp lệ.

Trạng thái ghi danh được cập nhật theo tiến trình:

```text
ELIGIBLE → ENROLLED → IN_PROGRESS → COMPLETED
                                  ↘ FAILED
```

`COMPLETED` và `FAILED` là các trạng thái kết thúc nhưng vẫn cho phép chuyển qua lại khi admin sửa nhầm. `WITHDRAWN` chỉ được giữ để đọc dữ liệu cũ và không còn là lựa chọn mới trên giao diện.

Sinh viên đã có đề tài/nhóm hoặc đã được giảng viên phân công vẫn có thể được ghi danh; việc ghi danh là hồ sơ tham dự đợt bảo vệ, không thay thế quan hệ nhóm–đề tài.

## 10. Hội đồng bảo vệ

Tạo hội đồng và thêm thành viên là **một flow liên tục**: tạo hội đồng ở bản nháp,
thêm đủ giảng viên, kiểm tra cơ cấu, rồi mới kích hoạt. Dữ liệu được lưu trong
`DefenseCommitteesEntity` và `ComitteesMemberEntity`.

### 10.1. Sơ đồ flow tạo hội đồng và thành viên

```text
Admin/Faculty chọn DefensePeriod chưa FINISHED
   │ POST /defense-periods/{defensePeriodId}/committees
   ▼
DefenseCommitteeService.create
   ├─ tìm đợt bảo vệ
   ├─ kiểm tra tên không trống + không trùng trong đợt
   └─ tạo DefenseCommitteesEntity(DRAFT, createdBy, createdAt)
          │
          ├─ POST /defense-committees/{committeeId}/members
          │      └─ CommitteeMemberService.assign
          │          ├─ hội đồng còn DRAFT, đợt chưa FINISHED
          │          ├─ giảng viên tồn tại + tài khoản ACTIVE
          │          ├─ không trùng giảng viên trong hội đồng
          │          ├─ không trùng CHAIRPERSON/SECRETARY ACTIVE
          │          └─ tạo ComitteesMemberEntity(ACTIVE, assignedBy, assignedAt)
          │
          ├─ GET /defense-committees/{id}/validation
          │      └─ kiểm tra cơ cấu thành viên
          │
          └─ PATCH /defense-committees/{id}/activate
                 └─ nếu hợp lệ → ACTIVE + activatedAt
```

### 10.2. API tạo và quản lý hội đồng

| Thao tác | API | Quyền | Xử lý |
| --- | --- | --- | --- |
| Tạo | `POST /defense-periods/{defensePeriodId}/committees` | Admin/Faculty | Nhận `committeeName`, `description`; kiểm tra đợt chưa kết thúc và tên duy nhất; tạo `DRAFT`. |
| Danh sách theo đợt | `GET /defense-periods/{defensePeriodId}/committees?status&keyword&page&size` | Đã đăng nhập | Lọc theo trạng thái/từ khóa/phân trang. |
| Chi tiết | `GET /defense-committees/{committeeId}` | Đã đăng nhập | Trả hội đồng và thông tin hiển thị. |
| Sửa | `PUT /defense-committees/{committeeId}` | Admin/Faculty | Chỉ hội đồng `DRAFT`; cập nhật tên/mô tả, kiểm tra trùng. |
| Kiểm tra | `GET /defense-committees/{committeeId}/validation` | Đã đăng nhập | Trả số thành viên ACTIVE, số Chủ tịch/Thư ký/Phản biện và danh sách lỗi. |
| Kích hoạt | `PATCH /defense-committees/{committeeId}/activate` | Admin/Faculty | Chỉ `DRAFT`, đợt chưa kết thúc và validation hợp lệ. |
| Đưa về nháp | `PATCH /defense-committees/{committeeId}/draft` | Admin/Faculty | Chỉ `ACTIVE` và chưa có lịch không bị hủy sử dụng hội đồng. |
| Ngừng hoạt động | `PATCH /defense-committees/{committeeId}/deactivate` | Admin/Faculty | Nhập lý do; không cho ngừng khi đang có lịch sử dụng. |
| Xóa | `DELETE /defense-committees/{committeeId}` | Admin/Faculty | Chỉ xóa `DRAFT` chưa có thành viên và chưa có lịch. |

### 10.3. API thêm và quản lý thành viên

| Thao tác | API | Quyền | Xử lý |
| --- | --- | --- | --- |
| Thêm thành viên | `POST /defense-committees/{committeeId}/members` | Admin/Faculty | Body `lectureId`, `role`, `note`; tạo thành viên ACTIVE. |
| Danh sách thành viên | `GET /defense-committees/{committeeId}/members?page&size` | Đã đăng nhập | Xem thành viên theo hội đồng. |
| Phân công của giảng viên | `GET /lecturers/{lectureId}/committee-assignments?page&size` | Admin/Faculty | Tra cứu các hội đồng của một giảng viên. |
| Thành viên hiện tại | `GET /committee-members/me?page&size` | Admin/Faculty/Reviewer/Supervisor | Giảng viên xem các hội đồng mình tham gia. |
| Sửa vai trò/ghi chú | `PATCH /committee-members/{memberId}` | Admin/Faculty | Chỉ thành viên ACTIVE và hội đồng DRAFT. |
| Ngừng thành viên | `PATCH /committee-members/{memberId}/deactivate` | Admin/Faculty | Bắt buộc lý do; chuyển INACTIVE và ghi `endedAt`. |

Vai trò hợp lệ là `CHAIRPERSON`, `SECRETARY`, `REVIEWER`, `MEMBER`. Backend không
cho phép hai Chủ tịch hoặc hai Thư ký ACTIVE trong cùng hội đồng, không cho trùng
giảng viên ACTIVE, và chỉ nhận giảng viên có tài khoản ACTIVE.

### 10.4. Kiểm tra trước khi kích hoạt

Hàm validation yêu cầu đồng thời:

1. Có **đúng một** Chủ tịch ACTIVE.
2. Có **đúng một** Thư ký ACTIVE.
3. Có ít nhất một Phản biện ACTIVE.
4. Có tối thiểu ba thành viên ACTIVE tổng cộng.
5. Mọi thành viên ACTIVE đều liên kết tới hồ sơ giảng viên và tài khoản ACTIVE.

Nếu thiếu điều kiện, API activate trả `1145` và response validation cho biết từng
lỗi; frontend phải hiển thị lỗi cụ thể để Admin bổ sung thành viên. Khi kích hoạt
thành công, hội đồng chuyển `DRAFT → ACTIVE`, lưu `activatedAt` và có thể được
chọn khi tạo lịch bảo vệ.

### 10.5. Vòng đời, lỗi và transaction

```text
DRAFT ──activate (validation hợp lệ)──→ ACTIVE
  ▲                                      │
  └─moveToDraft (chưa có lịch dùng)─────┘
  ACTIVE ──deactivate (có lý do)──────→ INACTIVE
```

Các lỗi chính:

| Điều kiện | Mã lỗi | HTTP |
| --- | ---: | :---: |
| Đợt bảo vệ/hội đồng không tồn tại | `1051`/`1134` | 404 |
| Đợt đã kết thúc | `1082` | 409 |
| Tên hội đồng trống | `1143` | 400 |
| Tên hội đồng trùng trong đợt | `1144` | 409 |
| Hội đồng không ở trạng thái cho phép thao tác | `1146` | 409 |
| Hội đồng chưa đủ cơ cấu để kích hoạt | `1145` | 409 |
| Hội đồng đang được lịch sử dụng | `1147` | 409 |
| Hội đồng chưa gắn đợt hợp lệ | `1142` | 409 |
| Giảng viên không tồn tại/không hoạt động | `1021`/`1132` | 404/409 |
| Giảng viên đã có trong hội đồng | `1137` | 409 |
| Đã có Chủ tịch hoặc Thư ký | `1138`/`1139` | 409 |
| Thành viên không còn ACTIVE | `1140` | 409 |
| Thiếu lý do ngừng hội đồng/thành viên | `1148`/`1141` | 400 |

Tạo hội đồng, thêm thành viên và kích hoạt là các transaction riêng; mỗi request
chỉ thành công khi toàn bộ bước của request đó commit. Việc kích hoạt không tự
động thêm thành viên còn thiếu. Khi một Admin khác vừa thêm thành viên hoặc kích
hoạt hội đồng, request sau phải nhận `409` và frontend cần tải lại validation cùng
danh sách thành viên thay vì ghi đè dữ liệu cũ.

## 11. Lập và quản lý lịch bảo vệ

### 11.1. Tạo lịch

Admin chọn một hoặc nhiều đề tài trong cùng buổi bảo vệ, sau đó nhập:

- Hội đồng đang hoạt động.
- Ngày bảo vệ.
- Giờ bắt đầu/kết thúc cho từng đề tài.
- Phòng và địa điểm.
- Buổi bảo vệ và ghi chú.

Đề tài chỉ xuất hiện trong danh sách nếu:

- Thuộc đúng đợt bảo vệ.
- Đã được phê duyệt/đăng ký hợp lệ.
- Đã có nhóm.
- Có phân công giảng viên hướng dẫn chính đang hoạt động.
- Chưa nằm trong một lịch bảo vệ khác.

Backend kiểm tra giờ không hợp lệ, trùng phòng, trùng hội đồng, trùng giảng viên và các xung đột liên quan trước khi lưu. Có thể gọi chức năng kiểm tra trước để hiển thị lỗi cho admin.

### 11.2. Trạng thái lịch

```text
DRAFT → PUBLISHED → COMPLETED
   ↘ POSTPONED → PUBLISHED
   ↘ CANCELLED
```

- `DRAFT`: được sửa hoặc xóa.
- `PUBLISHED`: lịch đã công bố và hiển thị cho giảng viên/sinh viên.
- `POSTPONED`: tạm hoãn, bắt buộc nhập lý do; có thể tiếp tục hoặc lập lại lịch.
- `COMPLETED`: buổi bảo vệ đã hoàn thành.
- `CANCELLED`: lịch bị hủy, bắt buộc nhập lý do.

Admin có thể mở chi tiết lịch để xem đề tài, nhóm, sinh viên, hội đồng, thành viên hội đồng, phòng, địa điểm và ghi chú. Mọi thay đổi quan trọng được ghi vào **Lịch sử lịch bảo vệ**.

## 12. Biểu mẫu, thư viện và kho lịch sử

- **Biểu mẫu**: tạo, cập nhật, xóa và cung cấp liên kết tải mẫu phiếu/biểu mẫu.
- **Thư viện đề tài**: lưu các đề tài tham khảo, tìm kiếm và quản lý nội dung thư viện.
- **Kho lịch sử**: tra cứu các dữ liệu/hoạt động đồ án đã kết thúc theo khả năng backend.
- Không xóa dữ liệu lịch sử nếu thao tác đó làm mất dấu vết quản trị hoặc vi phạm ràng buộc đang được sử dụng.

## 13. Nhật ký hoạt động và kiểm tra sau thao tác

Admin có thể xem **Nhật ký hoạt động** theo thời gian, người dùng hoặc hành động. Nhật ký được tạo cho các thao tác quản trị quan trọng như:

- Tạo/cập nhật/xóa dữ liệu.
- Duyệt hoặc từ chối đề tài.
- Phân công/ngừng phân công.
- Thay đổi hội đồng và lịch bảo vệ.
- Cập nhật role người dùng.

Sau mỗi thao tác thành công, frontend cần tải lại dữ liệu hoặc cập nhật state từ response API để badge/trạng thái hiển thị đúng. Toast thành công không thay thế việc xác nhận dữ liệu đã được lưu.

## 14. Thứ tự vận hành khuyến nghị

1. Tạo năm học.
2. Tạo đợt bảo vệ.
3. Tạo ngành và lớp.
4. Import/tạo sinh viên và giảng viên.
5. Tạo nhóm, thêm thành viên.
6. Tạo hoặc tiếp nhận đề tài.
7. Duyệt đề tài/đăng ký đề tài sinh viên.
8. Gán đề tài cho nhóm nếu cần.
9. Phân công giảng viên hướng dẫn.
10. Phân công giảng viên phản biện.
11. Tạo và mở milestone.
12. Theo dõi bài nộp, nhận xét và phê duyệt.
13. Ghi danh sinh viên vào đợt bảo vệ.
14. Tạo hội đồng, thêm thành viên và kích hoạt.
15. Tạo lịch cho các đề tài đủ điều kiện, kiểm tra xung đột và công bố.
16. Hoàn thành/tạm hoãn/hủy lịch khi phát sinh.
17. Kiểm tra nhật ký hoạt động và kho lịch sử.

## 15. Nguyên tắc nghiệp vụ quan trọng

- Frontend chỉ ẩn/hiện thao tác để hỗ trợ người dùng; backend vẫn là nơi quyết định quyền và điều kiện hợp lệ.
- Đề tài chưa được phê duyệt, chưa có nhóm hoặc chưa có giảng viên hướng dẫn không được đưa vào lịch bảo vệ.
- Một sinh viên chỉ thuộc một nhóm tại một thời điểm.
- Một nhóm chỉ có một đề tài đang thực hiện trong đợt.
- Một sinh viên không được ghi danh trùng cùng một đợt bảo vệ.
- Thao tác từ chối, hủy, tạm hoãn hoặc ngừng phân công phải lưu lý do khi API yêu cầu.
- Không dùng `alert` cho thao tác nghiệp vụ quan trọng; dùng modal xác nhận và toast/thông báo tiếng Việt.
- Khi API lỗi, hiển thị thông báo tiếng Việt và giữ nguyên dữ liệu người dùng đang nhập nếu có thể.
