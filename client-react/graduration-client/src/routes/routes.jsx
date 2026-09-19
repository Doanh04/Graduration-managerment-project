import React from 'react';
import { Navigate } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute.jsx';
import { PublicLoginRoute, RoleHome } from '../components/navigation/RoleHome.jsx';
import ApplicationLayout from '../layout/ApplicationLayout.jsx';
import DashboardPage from '../pages/shared/DashboardPage.jsx';
import ModulePage from '../pages/shared/ModulePage.jsx';
import PeriodResourcePage from '../pages/shared/PeriodResourcePage.jsx';
import LibraryTopicPage from '../pages/shared/LibraryTopicPage.jsx';
import StudentHomePage from '../pages/student/StudentHomePage.jsx';
import StudentTeamPage from '../pages/student/StudentTeamPage.jsx';
import RolePermissionPage from '../pages/admin/RolePermissionPage.jsx';
import SupervisorAssignmentPage from '../pages/admin/SupervisorAssignmentPage.jsx';
import SupervisorWorkspacePage from '../pages/lecturer/SupervisorWorkspacePage.jsx';
import ProjectHistoryPage from '../pages/admin/ProjectHistoryPage.jsx';
import AuditLogPage from '../pages/admin/AuditLogPage.jsx';
import MilestonePage from '../pages/shared/MilestonePage.jsx';
import SubmissionPage from '../pages/shared/SubmissionPage.jsx';
import ReviewAssignmentPage from '../pages/shared/ReviewAssignmentPage.jsx';
import DefenseProtectionPage from '../pages/lecturer/DefenseProtectionPage.jsx';
import TopicRegistrationPage from '../pages/shared/TopicRegistrationPage.jsx';
import GraduationEnrollmentPage from '../pages/shared/GraduationEnrollmentPage.jsx';
import DefenseCommitteePage from '../pages/admin/DefenseCommitteePage.jsx';
import { AccessDeniedPage, NotFoundPage } from '../pages/shared/SystemPages.jsx';
import API_ENDPOINTS from '../config/endpoints.js';

const guard = (element, options) => <ProtectedRoute {...options}>{element}</ProtectedRoute>;
const modulePage = (title, description, actionLabel, endpoint, columns, emptyMessage, searchOptions = {}) => <ModulePage title={title} description={description} actionLabel={actionLabel} endpoint={endpoint} columns={columns} emptyMessage={emptyMessage} {...searchOptions} />;

const columns = {
  students: [{ key: 'studentCode', label: 'Mã sinh viên' }, { key: 'fullName', label: 'Họ và tên', primary: true, secondary: 'email' }, { key: 'userName', label: 'Tài khoản' }, { key: 'createAt', label: 'Ngày tạo', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  lecturers: [{ key: 'lecturerCode', label: 'Mã giảng viên' }, { key: 'fullName', label: 'Họ và tên', primary: true, secondary: 'email' }, { key: 'degree', label: 'Học vị' }, { key: 'createAt', label: 'Ngày tạo', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  topics: [{ key: 'topicId', label: 'Mã' }, { key: 'title', label: 'Tên đề tài', primary: true, secondary: 'technology' }, { key: 'categoryTopic', label: 'Nguồn đề xuất', render: (value) => value === 'STUDENT' ? 'Sinh viên đề xuất' : value === 'LECTURER' ? 'Giảng viên đề xuất' : '—' }, { key: 'teamName', label: 'Nhóm' }, { key: 'updatedAt', label: 'Cập nhật', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  milestones: [{ key: 'milestoneId', label: 'Mã' }, { key: 'milestoneName', label: 'Tên milestone', primary: true, secondary: 'defensePeriodName' }, { key: 'milestoneType', label: 'Loại' }, { key: 'deadline', label: 'Deadline', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  templates: [{ key: 'templateId', label: 'Mã' }, { key: 'templateName', label: 'Tên biểu mẫu', primary: true, secondary: 'description' }, { key: 'templateType', label: 'Loại biểu mẫu', render: (value) => templateTypeLabel(value) }, { key: 'fileName', label: 'Tệp', render: (value) => value ? <span className="file-link">{value}</span> : '—' }, { key: 'createAt', label: 'Ngày tạo', format: 'date' }],
  academicYears: [{ key: 'academicId', label: 'Mã' }, { key: 'academicYear', label: 'Năm học', primary: true }, { key: 'startDate', label: 'Bắt đầu', format: 'date' }, { key: 'endDate', label: 'Kết thúc', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  periods: [{ key: 'defensePeriodId', label: 'Mã' }, { key: 'periodName', label: 'Đợt bảo vệ', primary: true, secondary: 'academicYear' }, { key: 'projectType', label: 'Loại đồ án' }, { key: 'startDate', label: 'Bắt đầu', format: 'date' }, { key: 'endDate', label: 'Kết thúc', format: 'date' }, { key: 'status', label: 'Trạng thái', format: 'status' }],
  majors: [{ key: 'majorId', label: 'Mã' }, { key: 'majorName', label: 'Tên ngành', primary: true, secondary: 'description' }, { key: 'majorCode', label: 'Mã ngành' }],
  classes: [{ key: 'classCode', label: 'Mã lớp' }, { key: 'className', label: 'Tên lớp', primary: true, secondary: 'description' }, { key: 'majorName', label: 'Ngành học' }],
  teams: [{ key: 'idTeam', label: 'Mã' }, { key: 'nameTeam', label: 'Tên nhóm', primary: true, secondary: 'description' }, { key: 'defensePeriodName', label: 'Đợt bảo vệ', secondary: 'academicYear' }, { key: 'topicTitle', label: 'Đề tài' }, { key: 'joinDate', label: 'Ngày tạo', format: 'date' }, { key: 'role', label: 'Vai trò' }],
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
const templateTypeOptions = [
  { value: 'TOPIC_REGISTRATION_FORM', label: 'Phiếu đăng ký đề tài' },
  { value: 'ASSIGNMENT_FORM', label: 'Phiếu phân công' },
  { value: 'REVIEW_FORM', label: 'Phiếu nhận xét' },
  { value: 'PROJECT_DIARY', label: 'Nhật ký đồ án' },
  { value: 'PROGRESS_REPORT', label: 'Báo cáo tiến độ' },
  { value: 'FINAL_REPORT', label: 'Báo cáo cuối kỳ' },
  { value: 'DEFENSE_SLIDE', label: 'Slide bảo vệ' },
  { value: 'SCORE_SHEET', label: 'Phiếu chấm điểm' },
  { value: 'OTHER', label: 'Khác' },
];

// Chuyển mã phân loại do backend trả về thành nhãn tiếng Việt để hiển thị trên bảng.
function templateTypeLabel(value) {
  return templateTypeOptions.find((option) => option.value === value)?.label
    || (value ? String(value).replaceAll('_', ' ') : 'Khác');
}

const academicYearOptions = Array.from({ length: 13 }, (_, index) => currentYear + index).map((startYear) => ({
  value: `${startYear}-${startYear + 1}`,
  label: `${startYear} - ${startYear + 1}`,
}));
const crud = {
  students: { idKey: 'idUser', create: API_ENDPOINTS.students.create, update: API_ENDPOINTS.students.update, updateMethod: 'patch', remove: API_ENDPOINTS.students.remove, removeKey: 'userName', resetPassword: API_ENDPOINTS.students.resetPassword, fields: [text('userName', 'Tên đăng nhập', true), text('password', 'Mật khẩu', true, { type: 'password', createOnly: true }), text('studentCode', 'Mã sinh viên', true), text('fullName', 'Họ và tên', true), text('email', 'Email', true, { type: 'email' }), text('phone', 'Số điện thoại', false, { inputMode: 'numeric', digitsOnly: true, maxLength: 10 }), text('classCode', 'Mã lớp', true, { type: 'searchable-select', endpoint: API_ENDPOINTS.classes.list, optionValue: 'classCode', optionLabel: 'className', optionSecondary: 'classCode', placeholder: 'Tìm theo tên hoặc mã lớp...' })] },
  lecturers: { idKey: 'userId', create: API_ENDPOINTS.lecturers.create, update: API_ENDPOINTS.lecturers.update, updateMethod: 'patch', remove: API_ENDPOINTS.lecturers.remove, resetPassword: API_ENDPOINTS.lecturers.resetPassword, fields: [text('userName', 'Tên đăng nhập', true), text('password', 'Mật khẩu', false, { type: 'password' }), text('lectureCode', 'Mã giảng viên', true, { source: 'lecturerCode' }), text('fullName', 'Họ và tên', true), text('degree', 'Học vị'), text('email', 'Email', true, { type: 'email' }), text('phone', 'Số điện thoại', false, { inputMode: 'numeric', digitsOnly: true, maxLength: 10 })] },
  topics: { resourceType: 'topic', idKey: 'topicId', create: API_ENDPOINTS.topics.create, multipart: true, previewField: 'fileName', previewEndpoint: API_ENDPOINTS.topics.file, update: API_ENDPOINTS.topics.update, updateMethod: 'patch', remove: API_ENDPOINTS.topics.remove, quickStatus: { field: 'status', options: [{ value: 'APPROVED', label: 'Phê duyệt', from: ['DRAFT', 'PENDING_APPROVAL'], endpoint: API_ENDPOINTS.topics.approve }, { value: 'REJECTED', label: 'Từ chối', from: ['PENDING_APPROVAL'], endpoint: API_ENDPOINTS.topics.reject, requiresReason: true }] }, fields: [text('title', 'Tên đề tài', true), text('description', 'Mô tả', false, { type: 'textarea' }), text('objective', 'Mục tiêu', false, { type: 'textarea' }), text('technology', 'Công nghệ'), text('categoryTopic', 'Nguồn đề xuất', true, { type: 'select', options: [{ value: 'LECTURER', label: 'Giảng viên đề xuất' }, { value: 'STUDENT', label: 'Sinh viên đề xuất' }] }), text('defensePeriodId', 'Đợt bảo vệ', true, { type: 'searchable-select', endpoint: API_ENDPOINTS.defensePeriods.list, optionValue: 'defensePeriodId', optionLabel: 'periodName', optionSecondary: 'academicYear', optionEntity: 'đợt bảo vệ chưa kết thúc', optionFilter: (period) => period.status !== 'FINISHED', placeholder: 'Tìm và chọn đợt bảo vệ chưa kết thúc...' }), text('file', 'Tệp đề tài', false, { type: 'file', accept: '.pdf,.doc,.docx', help: 'Chấp nhận PDF, DOC hoặc DOCX; dung lượng tối đa 10 MB.' })] },
  academicYears: { idKey: 'academicId', create: API_ENDPOINTS.academicYears.create, update: API_ENDPOINTS.academicYears.update, remove: API_ENDPOINTS.academicYears.remove, fields: [text('academicYear', 'Năm học', true, { type: 'select', options: academicYearOptions }), text('description', 'Mô tả', false, { type: 'textarea' })] },
  periods: { idKey: 'defensePeriodId', create: API_ENDPOINTS.defensePeriods.create, update: API_ENDPOINTS.defensePeriods.update, remove: API_ENDPOINTS.defensePeriods.remove, quickStatus: { field: 'status', options: [{ value: 'PENDING', label: 'Chờ diễn ra' }, { value: 'ONGOING', label: 'Đang diễn ra' }, { value: 'FINISHED', label: 'Đã kết thúc' }] }, fields: [text('periodName', 'Tên đợt bảo vệ', true), text('startDate', 'Ngày bắt đầu', true, { type: 'date' }), text('endDate', 'Ngày kết thúc', true, { type: 'date' }), text('projectType', 'Loại đồ án', true), text('status', 'Trạng thái', true, { type: 'select', defaultValue: 'PENDING', options: [{ value: 'PENDING', label: 'Chờ diễn ra' }, { value: 'ONGOING', label: 'Đang diễn ra' }, { value: 'FINISHED', label: 'Đã kết thúc' }] }), text('academicId', 'Năm học', true, { type: 'searchable-select', endpoint: API_ENDPOINTS.academicYears.list, optionValue: 'academicId', optionLabel: 'academicYear', optionEntity: 'năm học', placeholder: 'Tìm và chọn năm học...' })] },
  majors: { idKey: 'majorId', create: API_ENDPOINTS.majors.create, update: API_ENDPOINTS.majors.update, remove: API_ENDPOINTS.majors.remove, fields: [text('majorCode', 'Mã ngành', true), text('majorName', 'Tên ngành', true), text('description', 'Mô tả', false, { type: 'textarea' })] },
  classes: { idKey: 'idClass', create: API_ENDPOINTS.classes.create, update: API_ENDPOINTS.classes.update, remove: API_ENDPOINTS.classes.remove, fields: [text('classCode', 'Mã lớp', true), text('nameClass', 'Tên lớp', true, { source: 'className' }), text('majorId', 'Ngành học', true, { type: 'searchable-select', endpoint: API_ENDPOINTS.majors.list, optionValue: 'majorId', optionLabel: 'majorName', placeholder: 'Tìm và chọn ngành học...' }), text('description', 'Mô tả', false, { type: 'textarea' })] },
  teams: { idKey: 'idTeam', create: API_ENDPOINTS.teams.create, detail: API_ENDPOINTS.teams.detail, detailView: 'team', addStudent: API_ENDPOINTS.teams.addStudent, addStudents: API_ENDPOINTS.teams.addStudents, studentsEndpoint: API_ENDPOINTS.students.list, update: API_ENDPOINTS.teams.update, remove: API_ENDPOINTS.teams.remove, fields: [text('defensePeriodId', 'Đợt bảo vệ', true, { type: 'searchable-select', endpoint: API_ENDPOINTS.defensePeriods.list, optionValue: 'defensePeriodId', optionLabel: 'periodName', optionSecondary: 'academicYear', optionEntity: 'đợt bảo vệ chưa kết thúc', optionFilter: (period) => period.status !== 'FINISHED', placeholder: 'Chọn đợt bảo vệ...' }), text('nameTeam', 'Tên nhóm', true), text('description', 'Mô tả', false, { type: 'textarea' }), text('joinDate', 'Ngày thành lập', false, { type: 'date' }), text('role', 'Vai trò'), text('topicId', 'Đề tài đã phê duyệt', false, { type: 'searchable-select', endpoint: API_ENDPOINTS.topics.list, optionValue: 'topicId', optionLabel: 'title', optionSecondary: 'technology', optionEntity: 'đề tài đã phê duyệt', optionFilter: (topic) => topic.status === 'APPROVED', placeholder: 'Chỉ chọn đề tài cùng đợt bảo vệ...' })] },
  templates: { idKey: 'templateId', create: API_ENDPOINTS.templates.create, multipart: true, previewField: 'fileName', previewEndpoint: API_ENDPOINTS.templates.file, update: API_ENDPOINTS.templates.update, remove: API_ENDPOINTS.templates.remove, fields: [text('templateName', 'Tên biểu mẫu', true), text('templateType', 'Loại biểu mẫu', true, { type: 'select', defaultValue: 'OTHER', options: templateTypeOptions }), text('description', 'Mô tả', false, { type: 'textarea' }), text('file', 'Tệp biểu mẫu', false, { type: 'file', requiredOnCreate: true, accept: '.pdf,.doc,.docx', help: 'Chấp nhận PDF, DOC hoặc DOCX; dung lượng tối đa 10 MB.' })] },
};

const committeeCrud = (periodId) => ({
  idKey: 'committeeId',
  create: API_ENDPOINTS.committees.create(periodId),
  update: API_ENDPOINTS.committees.update,
  remove: API_ENDPOINTS.committees.remove,
  fields: [
    text('committeeName', 'Tên hội đồng', true),
    text('description', 'Mô tả', false, { type: 'textarea' }),
  ],
});

const scheduleCrud = (periodId) => ({
  resourceType: 'schedule',
  idKey: 'scheduleId',
  detail: API_ENDPOINTS.schedules.detail,
  history: API_ENDPOINTS.schedules.history,
  detailView: 'schedule',
  create: API_ENDPOINTS.schedules.create(periodId),
  bulkCreate: API_ENDPOINTS.schedules.bulkCreate(periodId),
  update: API_ENDPOINTS.schedules.update,
  remove: API_ENDPOINTS.schedules.remove,
  quickStatus: { field: 'status', options: [
    { value: 'PUBLISHED', label: 'Kích hoạt lịch', from: ['DRAFT', 'POSTPONED'], endpoint: (id, row) => row?.status === 'POSTPONED' ? API_ENDPOINTS.schedules.resume(id) : API_ENDPOINTS.schedules.publish(id), method: 'patch' },
    { value: 'POSTPONED', label: 'Tạm hoãn', from: ['PUBLISHED', 'SCHEDULED'], endpoint: API_ENDPOINTS.schedules.postpone, method: 'patch', requiresReason: true },
    { value: 'COMPLETED', label: 'Đã hoàn thành', from: ['PUBLISHED', 'SCHEDULED'], endpoint: API_ENDPOINTS.schedules.complete, method: 'patch' },
    { value: 'CANCELLED', label: 'Hủy lịch', from: ['DRAFT', 'PUBLISHED', 'SCHEDULED', 'POSTPONED'], endpoint: API_ENDPOINTS.schedules.cancel, method: 'patch', requiresReason: true },
  ] },
  fields: [
    text('topicSchedules', 'Danh sách đề tài bảo vệ', true, {
      // Dùng endpoint danh sách đề tài vốn đã được triển khai; lọc theo đợt ở server
      // rồi tiếp tục áp dụng điều kiện nhóm/giảng viên ở giao diện để không phụ thuộc
      // vào endpoint mới khi backend đang chạy phiên bản cũ.
      type: 'schedule-topic-list', endpoint: API_ENDPOINTS.topics.list, params: { defensePeriodId: periodId, size: 100 }, createOnly: true,
      transform: (items) => items.map(({ topicId, startTime, endTime }) => ({ topicId, startTime, endTime })),
      optionValue: 'topicId', optionLabel: 'title', optionSecondary: 'teamName', optionEntity: 'đề tài đủ điều kiện',
      optionFilter: (topic) => ['APPROVED', 'REGISTERED', 'IN_PROGRESS'].includes(topic.status) && topic.teamId && topic.hasActiveSupervisor === true,
      placeholder: 'Tìm và chọn nhiều đề tài...',
    }),
    text('topicId', 'Đề tài bảo vệ', true, {
      type: 'searchable-select', endpoint: API_ENDPOINTS.topics.list, editOnly: true,
      optionValue: 'topicId', optionLabel: 'title', optionSecondary: 'teamName', optionEntity: 'đề tài đủ điều kiện',
      optionFilter: (topic) => String(topic.defensePeriodId) === String(periodId) && ['APPROVED', 'REGISTERED', 'IN_PROGRESS'].includes(topic.status) && topic.teamId && topic.hasActiveSupervisor === true,
      placeholder: 'Tìm và chọn đề tài...',
    }),
    text('committeeId', 'Hội đồng bảo vệ', true, {
      type: 'searchable-select', endpoint: API_ENDPOINTS.committees.byPeriod(periodId),
      optionValue: 'committeeId', optionLabel: 'committeeName', optionSecondary: 'status', optionEntity: 'hội đồng đang hoạt động',
      optionFilter: (committee) => committee.status === 'ACTIVE', placeholder: 'Tìm và chọn hội đồng đang hoạt động...',
    }),
    text('defenseDate', 'Ngày bảo vệ', true, { type: 'date' }),
    text('startTime', 'Giờ bắt đầu', true, { type: 'time', editOnly: true }),
    text('endTime', 'Giờ kết thúc', true, { type: 'time', editOnly: true }),
    text('room', 'Phòng bảo vệ', true),
    text('location', 'Địa điểm', true),
    text('session', 'Buổi bảo vệ', false, { type: 'select', options: [
      { value: 'MORNING', label: 'Buổi sáng' }, { value: 'AFTERNOON', label: 'Buổi chiều' }, { value: 'EVENING', label: 'Buổi tối' },
    ] }),
    text('note', 'Ghi chú', false, { type: 'textarea' }),
  ],
});

export const Routers = [
  { path: '/', element: <RoleHome /> },
  { path: '/login', element: <PublicLoginRoute /> },
  { path: '/access-denied', element: <AccessDeniedPage /> },
  {
    path: '/admin',
    element: guard(<ApplicationLayout section="admin" />, { allowedRoles: ['ADMIN', 'FACULTY'] }),
    children: [
      { index: true, element: <DashboardPage section="admin" /> },
      { path: 'students', element: <ModulePage title="Quản lý sinh viên" description="Danh sách sinh viên tham gia học phần đồ án tốt nghiệp." actionLabel="Thêm sinh viên" endpoint={API_ENDPOINTS.students.list} columns={columns.students} crud={crud.students} importEndpoint={API_ENDPOINTS.students.import} exportEndpoint={API_ENDPOINTS.students.export} exportAcademicYearsEndpoint={API_ENDPOINTS.academicYears.list} exportDefensePeriodsEndpoint={API_ENDPOINTS.defensePeriods.list} serverSearch searchFields={['fullName', 'studentCode']} searchPlaceholder="Tìm theo tên hoặc mã sinh viên..." filters={[{ name: 'academicYearId', label: 'Năm học', endpoint: API_ENDPOINTS.academicYears.list, optionValue: 'academicId', optionLabel: 'academicYear', placeholder: 'Tất cả năm học', resetFields: ['defensePeriodId'] }, { name: 'defensePeriodId', label: 'Đợt bảo vệ', endpoint: API_ENDPOINTS.defensePeriods.list, optionValue: 'defensePeriodId', optionLabel: 'periodName', optionSecondary: 'academicYear', placeholder: 'Tất cả đợt bảo vệ', filter: (period, values) => !values.academicYearId || String(period.academicId) === String(values.academicYearId) }]} /> },
      { path: 'lecturers', element: <ModulePage title="Quản lý giảng viên" description="Quản lý hồ sơ và tải hướng dẫn của giảng viên." actionLabel="Thêm giảng viên" endpoint={API_ENDPOINTS.lecturers.list} columns={columns.lecturers} crud={crud.lecturers} importEndpoint={API_ENDPOINTS.lecturers.import} serverSearch searchFields={['fullName', 'lecturerCode']} searchPlaceholder="Tìm theo tên hoặc mã giảng viên..." /> },
      { path: 'academic-years', element: <ModulePage title="Quản lý năm học" description="Thiết lập năm học cho các đợt đồ án tốt nghiệp." actionLabel="Thêm năm học" endpoint={API_ENDPOINTS.academicYears.list} columns={columns.academicYears} crud={crud.academicYears} /> },
      { path: 'defense-periods', element: <ModulePage title="Quản lý đợt bảo vệ" description="Thiết lập thời gian và trạng thái từng đợt bảo vệ." actionLabel="Thêm đợt bảo vệ" endpoint={API_ENDPOINTS.defensePeriods.list} columns={columns.periods} crud={crud.periods} /> },
      { path: 'majors', element: <ModulePage title="Quản lý ngành học" description="Danh sách ngành và dữ liệu đào tạo của khoa." actionLabel="Thêm ngành" endpoint={API_ENDPOINTS.majors.list} columns={columns.majors} crud={crud.majors} /> },
      { path: 'classes', element: <ModulePage title="Quản lý lớp học" description="Quản lý danh sách lớp và ngành học trực thuộc." actionLabel="Thêm lớp học" endpoint={API_ENDPOINTS.classes.list} columns={columns.classes} crud={crud.classes} searchFields={['className', 'classCode', 'majorName']} searchPlaceholder="Tìm theo tên lớp, mã lớp hoặc ngành..." /> },
      { path: 'teams', element: <ModulePage title="Quản lý nhóm sinh viên" description="Tạo và quản lý nhóm theo từng đợt bảo vệ; một sinh viên chỉ thuộc một nhóm trong cùng đợt." actionLabel="Tạo nhóm" endpoint={API_ENDPOINTS.teams.list} columns={columns.teams} crud={crud.teams} filters={[{ name: 'defensePeriodId', label: 'Đợt bảo vệ', endpoint: API_ENDPOINTS.defensePeriods.list, optionValue: 'defensePeriodId', optionLabel: 'periodName', optionSecondary: 'academicYear', placeholder: 'Tất cả đợt bảo vệ' }]} /> },
      { path: 'topics', element: <ModulePage title="Quản lý đề tài" description="Thêm, cập nhật, tra cứu và quản lý các đề tài đồ án tốt nghiệp." actionLabel="Tạo đề tài" endpoint={API_ENDPOINTS.topics.list} resourceParams={{ excludeStudentProposals: true }} columns={columns.topics} crud={{ ...crud.topics, detail: API_ENDPOINTS.topics.detail, detailView: 'topic' }} importEndpoint={API_ENDPOINTS.topics.import} serverSearch searchFields={['title', 'technology', 'teamName', 'status']} searchPlaceholder="Tìm theo tên đề tài, công nghệ, nhóm hoặc trạng thái..." filters={[{ name: 'defensePeriodId', label: 'Đợt bảo vệ', endpoint: API_ENDPOINTS.defensePeriods.list, optionValue: 'defensePeriodId', optionLabel: 'periodName', optionSecondary: 'academicYear', placeholder: 'Tất cả đợt bảo vệ' }]} /> },
      { path: 'supervisor-assignments', element: <SupervisorAssignmentPage /> },
      // Đường dẫn cũ được giữ để chuyển các bookmark về luồng hội đồng mới.
      { path: 'review-assignments', element: <Navigate to="/admin/committees" replace /> },
      { path: 'progress', element: <MilestonePage /> },
      { path: 'submissions', element: <SubmissionPage mode="review" /> },
      { path: 'topic-registrations', element: <TopicRegistrationPage mode="admin" /> },
      { path: 'graduation-enrollments', element: <GraduationEnrollmentPage mode="admin" /> },
      { path: 'committees', element: <DefenseCommitteePage /> },
      { path: 'schedules', element: <PeriodResourcePage title="Lịch bảo vệ" description="" actionLabel="Tạo lịch" endpointForPeriod={API_ENDPOINTS.schedules.byPeriod} crudForPeriod={scheduleCrud} columns={columns.schedules} /> },
      { path: 'templates', element: <ModulePage title="Kho biểu mẫu" description="Quản lý phiếu giao đề tài, nhận xét và mẫu báo cáo." actionLabel="Thêm biểu mẫu" endpoint={API_ENDPOINTS.templates.list} columns={columns.templates} crud={crud.templates} filters={[{ name: 'templateType', label: 'Loại biểu mẫu', options: templateTypeOptions, optionValue: 'value', optionLabel: 'label' }]} /> },
      { path: 'library-topics', element: <LibraryTopicPage canManage /> },
      { path: 'project-history', element: <ProjectHistoryPage /> },
      { path: 'permissions', element: <RolePermissionPage /> },
      { path: 'audit-logs', element: guard(<AuditLogPage />, { allowedRoles: ['ADMIN'] }) },
    ],
  },
  {
    path: '/lecturer',
    element: guard(<ApplicationLayout section="lecturer" />, { allowedAccountTypes: ['LECTURER'] }),
    children: [
      { index: true, element: <DashboardPage section="lecturer" /> },
      { path: 'supervision', element: <SupervisorWorkspacePage /> },
      { path: 'submissions', element: <SubmissionPage mode="review" /> },
      { path: 'reviews', element: <ReviewAssignmentPage /> },
      { path: 'schedules', element: <DefenseProtectionPage /> },
      { path: 'scores', element: <DefenseProtectionPage focus="scores" /> },
      { path: 'templates', element: <ModulePage title="Biểu mẫu" description="Xem và tải các biểu mẫu phục vụ hướng dẫn, phản biện và bảo vệ đồ án." actionLabel="Tải biểu mẫu" endpoint={API_ENDPOINTS.templates.list} columns={columns.templates} filters={[{ name: 'templateType', label: 'Loại biểu mẫu', options: templateTypeOptions, optionValue: 'value', optionLabel: 'label' }]} readOnly crud={{ idKey: 'templateId', previewField: 'fileName', previewEndpoint: API_ENDPOINTS.templates.file }} /> },
      { path: 'library-topics', element: <LibraryTopicPage /> },
    ],
  },
  {
    path: '/student',
    element: guard(<ApplicationLayout section="student" />, { allowedAccountTypes: ['STUDENT'] }),
    children: [
      { index: true, element: <StudentHomePage /> },
      { path: 'team', element: <StudentTeamPage /> },
      { path: 'topics', element: <TopicRegistrationPage mode="student" /> },
      { path: 'enrollment', element: <GraduationEnrollmentPage mode="student" /> },
      { path: 'progress', element: <SubmissionPage mode="student" /> },
      { path: 'schedule', element: <PeriodResourcePage title="Lịch bảo vệ" description="Thời gian, địa điểm, phòng và thông tin hội đồng." actionLabel="Xem lịch" endpointForPeriod={API_ENDPOINTS.schedules.byPeriod} columns={columns.schedules} /> },
      { path: 'results', element: modulePage('Kết quả đánh giá', 'Nhận xét, phản hồi và điểm số đã được công bố.', 'Xem kết quả', null, undefined, 'Kết quả sẽ hiển thị khi điểm được công bố.') },
      { path: 'templates', element: <ModulePage title="Biểu mẫu" description="Xem và tải các biểu mẫu dành cho sinh viên." actionLabel="Tải biểu mẫu" endpoint={API_ENDPOINTS.templates.list} columns={columns.templates} filters={[{ name: 'templateType', label: 'Loại biểu mẫu', options: templateTypeOptions, optionValue: 'value', optionLabel: 'label' }]} readOnly crud={{ idKey: 'templateId', previewField: 'fileName', previewEndpoint: API_ENDPOINTS.templates.file }} /> },
      { path: 'library-topics', element: <LibraryTopicPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
];

export default Routers;
