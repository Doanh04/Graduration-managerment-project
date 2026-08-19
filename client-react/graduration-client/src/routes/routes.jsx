import React from 'react';
import ProtectedRoute from '../components/ProtectedRoute.jsx';
import { PublicLoginRoute, RoleHome } from '../components/navigation/RoleHome.jsx';
import ApplicationLayout from '../layout/ApplicationLayout.jsx';
import DashboardPage from '../pages/shared/DashboardPage.jsx';
import ModulePage from '../pages/shared/ModulePage.jsx';
import PeriodResourcePage from '../pages/shared/PeriodResourcePage.jsx';
import LibraryTopicPage from '../pages/shared/LibraryTopicPage.jsx';
import StudentHomePage from '../pages/student/StudentHomePage.jsx';
import { AccessDeniedPage, NotFoundPage } from '../pages/shared/SystemPages.jsx';
import API_ENDPOINTS from '../config/endpoints.js';

const guard = (element, options) => <ProtectedRoute {...options}>{element}</ProtectedRoute>;
const modulePage = (title, description, actionLabel, endpoint, columns, emptyMessage, searchOptions = {}) => <ModulePage title={title} description={description} actionLabel={actionLabel} endpoint={endpoint} columns={columns} emptyMessage={emptyMessage} {...searchOptions} />;

const columns = {
  students: [{ key: 'studentCode', label: 'Mã sinh viên' }, { key: 'fullName', label: 'Họ và tên', primary: true, secondary: 'email' }, { key: 'userName', label: 'Tài khoản' }, { key: 'createAt', label: 'Ngày tạo', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  lecturers: [{ key: 'lecturerCode', label: 'Mã giảng viên' }, { key: 'fullName', label: 'Họ và tên', primary: true, secondary: 'email' }, { key: 'degree', label: 'Học vị' }, { key: 'createAt', label: 'Ngày tạo', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  topics: [{ key: 'topicId', label: 'Mã' }, { key: 'title', label: 'Tên đề tài', primary: true, secondary: 'technology' }, { key: 'teamName', label: 'Nhóm' }, { key: 'updatedAt', label: 'Cập nhật', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  milestones: [{ key: 'milestoneId', label: 'Mã' }, { key: 'milestoneName', label: 'Tên milestone', primary: true, secondary: 'defensePeriodName' }, { key: 'milestoneType', label: 'Loại' }, { key: 'deadline', label: 'Deadline', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  templates: [{ key: 'templateId', label: 'Mã' }, { key: 'templateName', label: 'Tên biểu mẫu', primary: true, secondary: 'description' }, { key: 'filePath', label: 'Tệp' }, { key: 'createAt', label: 'Ngày tạo', format: 'date' }],
  academicYears: [{ key: 'academicId', label: 'Mã' }, { key: 'academicYear', label: 'Năm học', primary: true }, { key: 'startDate', label: 'Bắt đầu', format: 'date' }, { key: 'endDate', label: 'Kết thúc', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  periods: [{ key: 'defensePeriodId', label: 'Mã' }, { key: 'periodName', label: 'Đợt bảo vệ', primary: true, secondary: 'academicYear' }, { key: 'startDate', label: 'Bắt đầu', format: 'date' }, { key: 'endDate', label: 'Kết thúc', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  majors: [{ key: 'majorId', label: 'Mã' }, { key: 'majorName', label: 'Tên ngành', primary: true, secondary: 'description' }, { key: 'majorCode', label: 'Mã ngành' }],
  classes: [{ key: 'classCode', label: 'Mã lớp' }, { key: 'className', label: 'Tên lớp', primary: true, secondary: 'description' }, { key: 'majorName', label: 'Ngành học' }],
  teams: [{ key: 'idTeam', label: 'Mã' }, { key: 'nameTeam', label: 'Tên nhóm', primary: true, secondary: 'description' }, { key: 'topicTitle', label: 'Đề tài' }, { key: 'joinDate', label: 'Ngày tạo', format: 'date' }, { key: 'role', label: 'Vai trò' }],
  supervisors: [{ key: 'lectureCode', label: 'Mã GV' }, { key: 'topicTitle', label: 'Đề tài hướng dẫn', primary: true, secondary: 'lectureName' }, { key: 'role', label: 'Vai trò' }, { key: 'assignedAt', label: 'Ngày phân công', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  submissions: [{ key: 'submissionId', label: 'Mã' }, { key: 'fileName', label: 'Bài nộp', primary: true, secondary: 'milestoneName' }, { key: 'teamName', label: 'Nhóm' }, { key: 'submittedAt', label: 'Thời gian nộp', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  reviews: [{ key: 'assignmentId', label: 'Mã' }, { key: 'topicTitle', label: 'Đề tài phản biện', primary: true, secondary: 'teamName' }, { key: 'deadline', label: 'Deadline', format: 'date' }, { key: 'recommendation', label: 'Đề xuất' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  committees: [{ key: 'memberId', label: 'Mã' }, { key: 'committeeName', label: 'Hội đồng', primary: true, secondary: 'lectureName' }, { key: 'role', label: 'Vai trò' }, { key: 'assignedAt', label: 'Phân công', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  defenseCommittees: [{ key: 'committeeId', label: 'Mã' }, { key: 'committeeName', label: 'Hội đồng', primary: true, secondary: 'description' }, { key: 'defensePeriodName', label: 'Đợt bảo vệ' }, { key: 'updatedAt', label: 'Cập nhật', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  schedules: [{ key: 'scheduleId', label: 'Mã' }, { key: 'topicTitle', label: 'Đề tài', primary: true, secondary: 'teamName' }, { key: 'room', label: 'Phòng' }, { key: 'defenseDate', label: 'Ngày bảo vệ', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  scores: [{ key: 'studentCode', label: 'Mã SV' }, { key: 'studentName', label: 'Sinh viên', primary: true, secondary: 'topicTitle' }, { key: 'totalScore', label: 'Điểm' }, { key: 'updatedAt', label: 'Cập nhật', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
};

const text = (name, label, required = false, extra = {}) => ({ name, label, required, ...extra });
const currentYear = new Date().getFullYear();
const academicYearOptions = Array.from({ length: 13 }, (_, index) => currentYear + index).map((startYear) => ({
  value: `${startYear}-${startYear + 1}`,
  label: `${startYear} - ${startYear + 1}`,
}));
const crud = {
  students: { idKey: 'idUser', create: API_ENDPOINTS.students.create, update: API_ENDPOINTS.students.update, updateMethod: 'patch', remove: API_ENDPOINTS.students.remove, removeKey: 'userName', resetPassword: API_ENDPOINTS.students.resetPassword, fields: [text('userName', 'Tên đăng nhập', true), text('password', 'Mật khẩu', true, { type: 'password', createOnly: true }), text('studentCode', 'Mã sinh viên', true), text('fullName', 'Họ và tên', true), text('email', 'Email', true, { type: 'email' }), text('phone', 'Số điện thoại', false, { inputMode: 'numeric', digitsOnly: true, maxLength: 10 }), text('classId', 'Lớp', true, { type: 'searchable-select', endpoint: API_ENDPOINTS.classes.list, optionValue: 'idClass', optionLabel: 'className', optionSecondary: 'classCode', placeholder: 'Tìm theo tên hoặc mã lớp...' })] },
  lecturers: { idKey: 'userId', create: API_ENDPOINTS.lecturers.create, update: API_ENDPOINTS.lecturers.update, updateMethod: 'patch', remove: API_ENDPOINTS.lecturers.remove, resetPassword: API_ENDPOINTS.lecturers.resetPassword, fields: [text('userName', 'Tên đăng nhập', true), text('password', 'Mật khẩu', false, { type: 'password' }), text('lectureCode', 'Mã giảng viên', true, { source: 'lecturerCode' }), text('fullName', 'Họ và tên', true), text('degree', 'Học vị'), text('email', 'Email', true, { type: 'email' }), text('phone', 'Số điện thoại', false, { inputMode: 'numeric', digitsOnly: true, maxLength: 10 })] },
  topics: { idKey: 'topicId', create: API_ENDPOINTS.topics.create, update: API_ENDPOINTS.topics.update, updateMethod: 'patch', remove: API_ENDPOINTS.topics.remove, quickStatus: { field: 'status', options: [{ value: 'PENDING_APPROVAL', label: 'Gửi chờ phê duyệt', from: ['DRAFT'], endpoint: API_ENDPOINTS.topics.submit }, { value: 'APPROVED', label: 'Phê duyệt', from: ['PENDING_APPROVAL'], endpoint: API_ENDPOINTS.topics.approve }, { value: 'REJECTED', label: 'Từ chối', from: ['PENDING_APPROVAL'], endpoint: API_ENDPOINTS.topics.reject, requiresReason: true }] }, fields: [text('title', 'Tên đề tài', true), text('description', 'Mô tả', false, { type: 'textarea' }), text('objective', 'Mục tiêu', false, { type: 'textarea' }), text('technology', 'Công nghệ'), text('categoryTopic', 'Nguồn đề xuất', true, { type: 'select', options: [{ value: 'LECTURER', label: 'Giảng viên đề xuất' }, { value: 'STUDENT', label: 'Sinh viên đề xuất' }] }), text('defensePeriodId', 'Đợt bảo vệ', true, { type: 'searchable-select', endpoint: API_ENDPOINTS.defensePeriods.list, optionValue: 'defensePeriodId', optionLabel: 'periodName', optionSecondary: 'academicYear', optionEntity: 'đợt bảo vệ', placeholder: 'Tìm và chọn đợt bảo vệ...' })] },
  academicYears: { idKey: 'academicId', create: API_ENDPOINTS.academicYears.create, update: API_ENDPOINTS.academicYears.update, remove: API_ENDPOINTS.academicYears.remove, fields: [text('academicYear', 'Năm học', true, { type: 'select', options: academicYearOptions }), text('description', 'Mô tả', false, { type: 'textarea' })] },
  periods: { idKey: 'defensePeriodId', create: API_ENDPOINTS.defensePeriods.create, update: API_ENDPOINTS.defensePeriods.update, remove: API_ENDPOINTS.defensePeriods.remove, quickStatus: { field: 'status', options: [{ value: 'PENDING', label: 'Chờ diễn ra' }, { value: 'ONGOING', label: 'Đang diễn ra' }, { value: 'FINISHED', label: 'Đã kết thúc' }] }, fields: [text('periodName', 'Tên đợt bảo vệ', true), text('startDate', 'Ngày bắt đầu', true, { type: 'date' }), text('endDate', 'Ngày kết thúc', true, { type: 'date' }), text('projectType', 'Loại đồ án'), text('status', 'Trạng thái', true, { type: 'select', options: [{ value: 'PENDING', label: 'Chờ diễn ra' }, { value: 'ONGOING', label: 'Đang diễn ra' }, { value: 'FINISHED', label: 'Đã kết thúc' }] }), text('academicId', 'Năm học', true, { type: 'searchable-select', endpoint: API_ENDPOINTS.academicYears.list, optionValue: 'academicId', optionLabel: 'academicYear', optionEntity: 'năm học', placeholder: 'Tìm và chọn năm học...' })] },
  majors: { idKey: 'majorId', create: API_ENDPOINTS.majors.create, update: API_ENDPOINTS.majors.update, remove: API_ENDPOINTS.majors.remove, fields: [text('majorName', 'Tên ngành', true), text('description', 'Mô tả', false, { type: 'textarea' })] },
  classes: { idKey: 'idClass', create: API_ENDPOINTS.classes.create, update: API_ENDPOINTS.classes.update, remove: API_ENDPOINTS.classes.remove, fields: [text('classCode', 'Mã lớp', true), text('nameClass', 'Tên lớp', true, { source: 'className' }), text('majorId', 'Ngành học', true, { type: 'searchable-select', endpoint: API_ENDPOINTS.majors.list, optionValue: 'majorId', optionLabel: 'majorName', placeholder: 'Tìm và chọn ngành học...' }), text('description', 'Mô tả', false, { type: 'textarea' })] },
  teams: { idKey: 'idTeam', create: API_ENDPOINTS.teams.create, detail: API_ENDPOINTS.teams.detail, detailView: 'team', addStudent: API_ENDPOINTS.teams.addStudent, addStudents: API_ENDPOINTS.teams.addStudents, studentsEndpoint: API_ENDPOINTS.students.list, update: API_ENDPOINTS.teams.update, remove: API_ENDPOINTS.teams.remove, fields: [text('nameTeam', 'Tên nhóm', true), text('description', 'Mô tả', false, { type: 'textarea' }), text('joinDate', 'Ngày thành lập', false, { type: 'date' }), text('role', 'Vai trò'), text('topicId', 'Đề tài đã phê duyệt', false, { type: 'searchable-select', endpoint: API_ENDPOINTS.topics.list, optionValue: 'topicId', optionLabel: 'title', optionSecondary: 'technology', optionEntity: 'đề tài đã phê duyệt', optionFilter: (topic) => topic.status === 'APPROVED', placeholder: 'Tìm theo tên đề tài hoặc công nghệ...' })] },
  templates: { idKey: 'templateId', create: API_ENDPOINTS.templates.create, update: API_ENDPOINTS.templates.update, remove: API_ENDPOINTS.templates.remove, fields: [text('templateName', 'Tên biểu mẫu', true), text('description', 'Mô tả', false, { type: 'textarea' }), text('filePath', 'Đường dẫn tệp', true)] },
};

export const Routers = [
  { path: '/', element: <RoleHome /> },
  { path: '/login', element: <PublicLoginRoute /> },
  { path: '/access-denied', element: <AccessDeniedPage /> },
  {
    path: '/admin',
    element: guard(<ApplicationLayout section="admin" />, { allowedRoles: ['ADMIN', 'FACULTY'] }),
    children: [
      { index: true, element: <DashboardPage section="admin" /> },
      { path: 'students', element: <ModulePage title="Quản lý sinh viên" description="Danh sách sinh viên tham gia học phần đồ án tốt nghiệp." actionLabel="Thêm sinh viên" endpoint={API_ENDPOINTS.students.list} columns={columns.students} crud={crud.students} importEndpoint={API_ENDPOINTS.students.import} exportEndpoint={API_ENDPOINTS.students.export} exportYearsEndpoint={API_ENDPOINTS.students.exportYears} serverSearch searchFields={['fullName', 'studentCode']} searchPlaceholder="Tìm theo tên hoặc mã sinh viên..." /> },
      { path: 'lecturers', element: <ModulePage title="Quản lý giảng viên" description="Quản lý hồ sơ và tải hướng dẫn của giảng viên." actionLabel="Thêm giảng viên" endpoint={API_ENDPOINTS.lecturers.list} columns={columns.lecturers} crud={crud.lecturers} importEndpoint={API_ENDPOINTS.lecturers.import} serverSearch searchFields={['fullName', 'lecturerCode']} searchPlaceholder="Tìm theo tên hoặc mã giảng viên..." /> },
      { path: 'academic-years', element: <ModulePage title="Quản lý năm học" description="Thiết lập năm học cho các đợt đồ án tốt nghiệp." actionLabel="Thêm năm học" endpoint={API_ENDPOINTS.academicYears.list} columns={columns.academicYears} crud={crud.academicYears} /> },
      { path: 'defense-periods', element: <ModulePage title="Quản lý đợt bảo vệ" description="Thiết lập thời gian và trạng thái từng đợt bảo vệ." actionLabel="Thêm đợt bảo vệ" endpoint={API_ENDPOINTS.defensePeriods.list} columns={columns.periods} crud={crud.periods} /> },
      { path: 'majors', element: <ModulePage title="Quản lý ngành học" description="Danh sách ngành và dữ liệu đào tạo của khoa." actionLabel="Thêm ngành" endpoint={API_ENDPOINTS.majors.list} columns={columns.majors} crud={crud.majors} /> },
      { path: 'classes', element: <ModulePage title="Quản lý lớp học" description="Quản lý danh sách lớp và ngành học trực thuộc." actionLabel="Thêm lớp học" endpoint={API_ENDPOINTS.classes.list} columns={columns.classes} crud={crud.classes} searchFields={['className', 'classCode', 'majorName']} searchPlaceholder="Tìm theo tên lớp, mã lớp hoặc ngành..." /> },
      { path: 'teams', element: <ModulePage title="Quản lý nhóm sinh viên" description="Tổ chức nhóm, thành viên và đề tài được lựa chọn." actionLabel="Tạo nhóm" endpoint={API_ENDPOINTS.teams.list} columns={columns.teams} crud={crud.teams} /> },
      { path: 'topics', element: <ModulePage title="Quản lý đề tài" description="Thêm, cập nhật, tra cứu và quản lý các đề tài đồ án tốt nghiệp." actionLabel="Tạo đề tài" endpoint={API_ENDPOINTS.topics.list} columns={columns.topics} crud={crud.topics} serverSearch searchFields={['title', 'technology', 'teamName', 'status']} searchPlaceholder="Tìm theo tên đề tài, công nghệ, nhóm hoặc trạng thái..." /> },
      { path: 'progress', element: modulePage('Tiến độ đồ án', 'Theo dõi milestone, deadline và tình trạng bài nộp toàn khoa.', 'Tạo milestone', API_ENDPOINTS.milestones.list, columns.milestones, undefined, { serverSearch: true }) },
      { path: 'committees', element: <PeriodResourcePage title="Hội đồng bảo vệ" description="Quản lý hội đồng và kiểm tra tính hợp lệ của thành viên." actionLabel="Tạo hội đồng" endpointForPeriod={API_ENDPOINTS.committees.byPeriod} columns={columns.defenseCommittees} /> },
      { path: 'schedules', element: <PeriodResourcePage title="Lịch bảo vệ" description="Sắp xếp thời gian, phòng, đề tài và xử lý xung đột lịch." actionLabel="Tạo lịch" endpointForPeriod={API_ENDPOINTS.schedules.byPeriod} columns={columns.schedules} /> },
      { path: 'templates', element: <ModulePage title="Kho biểu mẫu" description="Quản lý phiếu giao đề tài, nhận xét và mẫu báo cáo." actionLabel="Thêm biểu mẫu" endpoint={API_ENDPOINTS.templates.list} columns={columns.templates} crud={crud.templates} /> },
      { path: 'library-topics', element: <LibraryTopicPage canManage /> },
      { path: 'permissions', element: modulePage('Vai trò và phân quyền', 'Kiểm soát quyền truy cập chức năng trong hệ thống.', 'Tạo vai trò', API_ENDPOINTS.roles.list, undefined) },
    ],
  },
  {
    path: '/lecturer',
    element: guard(<ApplicationLayout section="lecturer" />, { allowedAccountTypes: ['LECTURER'] }),
    children: [
      { index: true, element: <DashboardPage section="lecturer" /> },
      { path: 'supervision', element: modulePage('Sinh viên hướng dẫn', 'Theo dõi nhóm, đề tài và tiến độ sinh viên được phân công.', 'Phân công mới', API_ENDPOINTS.supervisors.mine, columns.supervisors) },
      { path: 'submissions', element: modulePage('Duyệt báo cáo', 'Xem phiên bản bài nộp, nhận xét và yêu cầu chỉnh sửa.', 'Xem bài mới', API_ENDPOINTS.submissions.list, columns.submissions) },
      { path: 'reviews', element: modulePage('Phản biện đề tài', 'Quản lý các đề tài được phân công phản biện.', 'Mở phản biện', API_ENDPOINTS.reviews.mine, columns.reviews) },
      { path: 'schedules', element: modulePage('Lịch hội đồng', 'Lịch tham gia hội đồng và thông tin phòng bảo vệ.', 'Xem lịch', API_ENDPOINTS.committeeMembers.mine, columns.committees) },
      { path: 'scores', element: <PeriodResourcePage title="Chấm điểm" description="Nhập điểm theo tiêu chí và gửi kết quả đánh giá." actionLabel="Nhập điểm" endpointForPeriod={API_ENDPOINTS.scores.byPeriod} columns={columns.scores} /> },
    ],
  },
  {
    path: '/student',
    element: guard(<ApplicationLayout section="student" />, { allowedAccountTypes: ['STUDENT'] }),
    children: [
      { index: true, element: <StudentHomePage /> },
      { path: 'team', element: modulePage('Nhóm của tôi', 'Thông tin thành viên và phần công việc cá nhân của nhóm.', 'Cập nhật công việc', null, undefined, 'Backend hiện chưa có API “nhóm của tôi”.') },
      { path: 'topics', element: modulePage('Danh sách đề tài', 'Tra cứu, đăng ký hoặc đề xuất đề tài đồ án tốt nghiệp.', 'Đề xuất đề tài', API_ENDPOINTS.topics.list, columns.topics, undefined, { serverSearch: true }) },
      { path: 'progress', element: modulePage('Tiến độ và nộp bài', 'Theo dõi deadline, upload và lịch sử phiên bản bài nộp.', 'Nộp báo cáo', API_ENDPOINTS.milestones.list, columns.milestones, undefined, { serverSearch: true }) },
      { path: 'schedule', element: <PeriodResourcePage title="Lịch bảo vệ" description="Thời gian, địa điểm, phòng và thông tin hội đồng." actionLabel="Xem lịch" endpointForPeriod={API_ENDPOINTS.schedules.byPeriod} columns={columns.schedules} /> },
      { path: 'results', element: modulePage('Kết quả đánh giá', 'Nhận xét, phản hồi và điểm số đã được công bố.', 'Xem kết quả', null, undefined, 'Kết quả sẽ hiển thị khi điểm được công bố.') },
      { path: 'templates', element: modulePage('Biểu mẫu', 'Xem và tải các biểu mẫu dành cho sinh viên.', 'Tải biểu mẫu', API_ENDPOINTS.templates.list, columns.templates) },
      { path: 'library-topics', element: <LibraryTopicPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
];

export default Routers;
