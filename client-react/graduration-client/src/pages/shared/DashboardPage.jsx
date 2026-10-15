import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, ArrowRight, BookOpen, CalendarClock, CheckCircle2, Clock3, FileCheck2, LoaderCircle, TrendingUp, Users } from 'lucide-react';
import ResourceService from '../../services/ResourceService.jsx';
import API_ENDPOINTS from '../../config/endpoints.js';
import { formatDateTimeDisplay } from '../../utils/dateFormat.js';

const sectionCopy = {
  admin: ['Tổng quan học kỳ đồ án', 'Theo dõi tiến độ toàn khoa và xử lý các công việc quan trọng.'],
  lecturer: ['Không gian làm việc giảng viên', 'Theo dõi công việc hướng dẫn, phản biện và lịch hội đồng của bạn.'],
  student: ['Tiến độ đồ án của bạn', 'Theo dõi deadline, phản hồi và các bài nộp trong kỳ đồ án.'],
};

const sources = {
  admin: [
    { label: 'Sinh viên tham gia', endpoint: '/register-student/get-all-student', Icon: Users, color: 'blue', yearField: 'createAt' },
    { label: 'Giảng viên', endpoint: '/register-lecture/get-all-lecture', Icon: Users, color: 'cyan' },
    { label: 'Đề tài', endpoint: '/topics', Icon: BookOpen, color: 'green' },
    { label: 'Bài nộp', endpoint: '/submissions', Icon: FileCheck2, color: 'orange', yearField: 'submittedAt' },
  ],
  lecturer: [
    { label: 'Đề tài hướng dẫn', endpoint: '/topic-supervisors/me', Icon: Users, color: 'blue' },
    { label: 'Bài nộp cần duyệt', endpoint: '/submissions', Icon: FileCheck2, color: 'orange', params: { status: 'SUBMITTED' } },
    { label: 'Đề tài phản biện', endpoint: '/review-assignments/me', Icon: BookOpen, color: 'cyan' },
    { label: 'Lịch hội đồng', endpoint: '/committee-members/me', Icon: CalendarClock, color: 'green' },
  ],
  student: [
    { label: 'Mốc tiến độ', endpoint: '/milestones', Icon: TrendingUp, color: 'blue' },
    { label: 'Bài đã nộp', endpoint: '/submissions', Icon: CheckCircle2, color: 'green' },
    { label: 'Deadline đang mở', endpoint: '/milestones', Icon: Clock3, color: 'orange', params: { status: 'OPEN' } },
    { label: 'Biểu mẫu', endpoint: '/template/get-all-template', Icon: FileCheck2, color: 'cyan' },
  ],
};

export default function DashboardPage({ section }) {
  const currentYear = new Date().getFullYear();
  const [stats, setStats] = useState([]);
  const [milestones, setMilestones] = useState([]);
  const [defensePeriods, setDefensePeriods] = useState([]);
  const [supervisorAssignments, setSupervisorAssignments] = useState([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState('');
  const [resourceCounts, setResourceCounts] = useState({ libraryTopics: 0, templates: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true); setError(null);
      try {
        const [counts, milestonePage, periodItems, assignmentItems, adminResourceCounts] = await Promise.all([
          Promise.all(sources[section].map(async (source) => ({ ...source, value: source.yearField ? await countRecordsByYear(source, currentYear) : await ResourceService.count(source.endpoint, source.params) }))),
          ResourceService.getPage('/milestones', { page: 0, size: 6 }),
          section !== 'student' ? ResourceService.getAll(API_ENDPOINTS.defensePeriods.list) : Promise.resolve([]),
          section === 'admin'
            ? ResourceService.getAll(API_ENDPOINTS.supervisors.list)
            : section === 'lecturer' ? ResourceService.getAll(API_ENDPOINTS.supervisors.mine) : Promise.resolve([]),
          section !== 'student'
            ? Promise.all([ResourceService.count(API_ENDPOINTS.libraryTopics.list), ResourceService.count(API_ENDPOINTS.templates.list)])
            : Promise.resolve([0, 0]),
        ]);
        if (active) {
          const sortedPeriods = [...periodItems].sort(comparePeriodsNewestFirst);
          setStats(counts);
          setMilestones(milestonePage.content);
          setDefensePeriods(sortedPeriods);
          setSupervisorAssignments(assignmentItems);
          setResourceCounts({ libraryTopics: adminResourceCounts[0], templates: adminResourceCounts[1] });
          setSelectedPeriodId((current) => sortedPeriods.some((period) => String(period.defensePeriodId) === current)
            ? current
            : String(sortedPeriods[0]?.defensePeriodId ?? ''));
        }
      } catch (requestError) { if (active) setError(requestError); }
      finally { if (active) setLoading(false); }
    };
    load();
    return () => { active = false; };
  }, [section, currentYear]);

  const tasks = useMemo(() => milestones.filter((item) => item.status === 'OPEN' || item.status === 'DRAFT').sort((a, b) => new Date(a.deadline || 0) - new Date(b.deadline || 0)).slice(0, 4), [milestones]);
  const supervisorLoads = useMemo(() => buildSupervisorLoads(supervisorAssignments, selectedPeriodId), [supervisorAssignments, selectedPeriodId]);
  const guidedTopics = useMemo(() => buildGuidedTopics(supervisorAssignments, selectedPeriodId), [supervisorAssignments, selectedPeriodId]);
  const supervisedTopicCount = useMemo(() => new Set(supervisorLoads.flatMap((lecturer) => lecturer.topicIds)).size, [supervisorLoads]);
  const completed = milestones.filter((item) => item.status === 'CLOSED').length;
  const completion = milestones.length ? Math.round((completed / milestones.length) * 100) : 0;
  const [title, subtitle] = sectionCopy[section];

  return <div className="page-stack">
    <section className="welcome-card"><div><span className="welcome-kicker">HỆ THỐNG QUẢN LÝ ĐỒ ÁN TỐT NGHIỆP</span><h2>{title}</h2><p>{subtitle}</p></div><button className="primary-button">Xem kế hoạch <ArrowRight size={17} /></button></section>
    {loading ? <section className="panel dashboard-state"><LoaderCircle className="spin" size={28} /><span>Đang tổng hợp dữ liệu...</span></section> : error ? <section className="panel dashboard-state error"><AlertCircle size={28} /><span>{error.message || 'Không thể tải số liệu dashboard.'}</span></section> : <>
      <section className="stat-grid">{stats.map(({ label, value, Icon, color, yearField }) => <article className="stat-card" key={label}><div className={`stat-icon ${color}`}><Icon size={21} /></div><span>{label}</span><strong>{new Intl.NumberFormat('vi-VN').format(value)}</strong><small>{yearField ? `Dữ liệu trong năm ${currentYear}` : 'Dữ liệu cập nhật từ hệ thống'}</small></article>)}</section>
      <section className="dashboard-grid">
        {section === 'admin'
          ? <SupervisorLoadPanel periods={defensePeriods} selectedPeriodId={selectedPeriodId} onPeriodChange={setSelectedPeriodId} rows={supervisorLoads} topicCount={supervisedTopicCount} />
          : section === 'lecturer'
            ? <GuidedTopicPanel periods={defensePeriods} selectedPeriodId={selectedPeriodId} onPeriodChange={setSelectedPeriodId} topics={guidedTopics} />
            : <article className="panel activity-panel"><div className="panel-heading"><div><span className="panel-kicker">MỐC TIẾN ĐỘ</span><h3>Công việc sắp tới</h3></div></div>{tasks.length ? <div className="task-list">{tasks.map((task, index) => <div className="task-row" key={task.milestoneId}><span className="task-index">{String(index + 1).padStart(2, '0')}</span><div><strong>{task.milestoneName}</strong><small>{deadlineText(task.deadline)}</small></div><span className={`priority ${task.status === 'OPEN' ? 'high' : ''}`}>{formatStatus(task.status)}</span><ArrowRight className="row-arrow" size={17} /></div>)}</div> : <div className="inline-empty">Không có milestone đang chờ xử lý.</div>}</article>}
        {section !== 'student'
          ? <ResourceCountPanel counts={resourceCounts} />
          : <article className="panel progress-panel"><div className="panel-heading"><div><span className="panel-kicker">TIẾN ĐỘ</span><h3>Tình trạng milestone</h3></div></div><div className="overall-progress"><strong>{completion}%</strong><span>{completed}/{milestones.length} mốc đã đóng</span><div className="progress-track"><i style={{ width: `${completion}%` }} /></div></div>{tasks[0] && <div className="deadline-note"><Clock3 size={19} /><div><strong>Deadline gần nhất</strong><span>{tasks[0].milestoneName} · {formatDate(tasks[0].deadline)}</span></div></div>}</article>}
      </section>
    </>}
  </div>;
}

function ResourceCountPanel({ counts }) {
  return <article className="panel resource-count-panel">
    <div className="panel-heading"><div><span className="panel-kicker">TÀI NGUYÊN HỌC PHẦN</span><h3>Kho tài liệu hỗ trợ</h3></div></div>
    <div className="resource-count-grid">
      <div className="resource-count-card library">
        <span className="resource-count-icon"><BookOpen size={21} /></span>
        <div><small>ĐỀ TÀI THAM KHẢO</small><strong>{new Intl.NumberFormat('vi-VN').format(counts.libraryTopics)}</strong><span>Đề tài trong thư viện</span></div>
      </div>
      <div className="resource-count-card template">
        <span className="resource-count-icon"><FileCheck2 size={21} /></span>
        <div><small>TÀI LIỆU BIỂU MẪU</small><strong>{new Intl.NumberFormat('vi-VN').format(counts.templates)}</strong><span>Biểu mẫu đang có</span></div>
      </div>
    </div>
  </article>;
}

function GuidedTopicPanel({ periods, selectedPeriodId, onPeriodChange, topics }) {
  return <article className="panel activity-panel guided-topic-panel">
    <div className="panel-heading supervisor-load-heading">
      <div><span className="panel-kicker">ĐỀ TÀI HƯỚNG DẪN</span><h3>Đề tài đang phụ trách</h3></div>
      <label className="supervisor-period-filter">
        <span>Đợt bảo vệ</span>
        <select value={selectedPeriodId} onChange={(event) => onPeriodChange(event.target.value)} disabled={!periods.length}>
          {!periods.length && <option value="">Chưa có đợt bảo vệ</option>}
          {periods.map((period) => <option key={period.defensePeriodId} value={period.defensePeriodId}>{period.periodName}{period.academicYear ? ` · ${period.academicYear}` : ''}</option>)}
        </select>
      </label>
    </div>
    <div className="supervisor-load-summary"><span><strong>{topics.length}</strong> đề tài đang hướng dẫn trong đợt</span></div>
    {topics.length ? <div className="guided-topic-list">{topics.map((topic, index) => <div className="guided-topic-row" key={topic.topicId}>
      <span className="task-index">{String(index + 1).padStart(2, '0')}</span>
      <div className="guided-topic-info"><strong>{topic.title}</strong><small>{topic.assignedAt ? `Phân công: ${formatDateTimeDisplay(topic.assignedAt)}` : 'Đang được phân công hướng dẫn'}</small></div>
      <span className={`supervisor-role ${topic.role === 'PRIMARY' ? 'primary' : ''}`}>{topic.role === 'PRIMARY' ? 'Hướng dẫn chính' : 'Đồng hướng dẫn'}</span>
    </div>)}</div> : <div className="inline-empty">Bạn chưa được phân công hướng dẫn đề tài trong đợt này.</div>}
  </article>;
}

function SupervisorLoadPanel({ periods, selectedPeriodId, onPeriodChange, rows, topicCount }) {
  return <article className="panel activity-panel supervisor-load-panel">
    <div className="panel-heading supervisor-load-heading">
      <div><span className="panel-kicker">PHÂN CÔNG HƯỚNG DẪN</span><h3>Thống kê giảng viên hướng dẫn</h3></div>
      <label className="supervisor-period-filter">
        <span>Đợt bảo vệ</span>
        <select value={selectedPeriodId} onChange={(event) => onPeriodChange(event.target.value)} disabled={!periods.length}>
          {!periods.length && <option value="">Chưa có đợt bảo vệ</option>}
          {periods.map((period) => <option key={period.defensePeriodId} value={period.defensePeriodId}>{period.periodName}{period.academicYear ? ` · ${period.academicYear}` : ''}</option>)}
        </select>
      </label>
    </div>
    <div className="supervisor-load-summary"><span><strong>{rows.length}</strong> giảng viên đang hướng dẫn</span><span><strong>{topicCount}</strong> đề tài trong đợt</span></div>
    {rows.length ? <div className="supervisor-load-list">{rows.map((lecturer) => <div className="supervisor-load-row" key={lecturer.key}>
      <span className="supervisor-avatar">{initialsOf(lecturer.name)}</span>
      <div className="supervisor-info"><strong>{lecturer.name}</strong><small>{lecturer.code || 'Chưa có mã giảng viên'}</small><span>{formatSupervisorRoles(lecturer)}</span></div>
      <div className="supervisor-topic-count"><strong>{lecturer.topicIds.length}</strong><span>đề tài</span></div>
    </div>)}</div> : <div className="inline-empty">Chưa có giảng viên được phân công hướng dẫn trong đợt này.</div>}
  </article>;
}

function buildSupervisorLoads(assignments, periodId) {
  const lecturers = new Map();
  assignments
    .filter((item) => String(item.defensePeriodId) === String(periodId) && String(item.status || '').toUpperCase() === 'ACTIVE')
    .forEach((item) => {
      const key = String(item.lectureId || item.lectureCode || item.lectureName || item.assignmentId);
      if (!lecturers.has(key)) lecturers.set(key, { key, name: item.lectureName || 'Giảng viên chưa cập nhật tên', code: item.lectureCode || '', topics: new Map() });
      lecturers.get(key).topics.set(String(item.topicId), String(item.role || '').toUpperCase());
    });
  return [...lecturers.values()]
    .map(({ topics, ...lecturer }) => ({
      ...lecturer,
      topicIds: [...topics.keys()],
      primaryCount: [...topics.values()].filter((role) => role === 'PRIMARY').length,
      coSupervisorCount: [...topics.values()].filter((role) => role === 'CO_SUPERVISOR').length,
    }))
    .sort((left, right) => right.topicIds.length - left.topicIds.length || left.name.localeCompare(right.name, 'vi'));
}

function buildGuidedTopics(assignments, periodId) {
  const topics = new Map();
  assignments
    .filter((item) => String(item.defensePeriodId) === String(periodId) && String(item.status || '').toUpperCase() === 'ACTIVE')
    .forEach((item) => topics.set(String(item.topicId), {
      topicId: item.topicId,
      title: item.topicTitle || `Đề tài #${item.topicId}`,
      role: String(item.role || '').toUpperCase(),
      assignedAt: item.assignedAt,
    }));
  return [...topics.values()].sort((left, right) => new Date(right.assignedAt || 0) - new Date(left.assignedAt || 0) || left.title.localeCompare(right.title, 'vi'));
}

function formatSupervisorRoles(lecturer) {
  const roles = [];
  if (lecturer.primaryCount) roles.push(`Hướng dẫn chính: ${lecturer.primaryCount}`);
  if (lecturer.coSupervisorCount) roles.push(`Đồng hướng dẫn: ${lecturer.coSupervisorCount}`);
  return roles.join(' · ') || 'Đang tham gia hướng dẫn';
}

function comparePeriodsNewestFirst(left, right) {
  const leftDate = new Date(left.startDate || left.endDate || 0).getTime();
  const rightDate = new Date(right.startDate || right.endDate || 0).getTime();
  return rightDate - leftDate || Number(right.defensePeriodId || 0) - Number(left.defensePeriodId || 0);
}

function initialsOf(name) {
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
  return parts.slice(-2).map((part) => part[0]).join('').toUpperCase() || 'GV';
}

function formatStatus(value) { return value ? value.replaceAll('_', ' ') : '—'; }
function formatDate(value) { return value ? formatDateTimeDisplay(value) : 'Chưa đặt hạn'; }
function deadlineText(value) { if (!value) return 'Chưa có deadline'; const diff = new Date(value).getTime() - Date.now(); const days = Math.ceil(diff / 86400000); return days < 0 ? `Đã quá hạn ${Math.abs(days)} ngày` : `Còn ${days} ngày · ${formatDate(value)}`; }
async function countRecordsByYear(source, year) { const records = await ResourceService.getAll(source.endpoint, source.params); return records.filter((record) => { const rawDate = record?.[source.yearField]; if (!rawDate) return false; const parsed = new Date(rawDate); return !Number.isNaN(parsed.getTime()) && parsed.getFullYear() === year; }).length; }
