import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, ArrowRight, BookOpen, CalendarClock, CheckCircle2, Clock3, FileCheck2, LoaderCircle, TrendingUp, Users } from 'lucide-react';
import ResourceService from '../../services/ResourceService.jsx';

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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true); setError(null);
      try {
        const [counts, milestonePage] = await Promise.all([
          Promise.all(sources[section].map(async (source) => ({ ...source, value: source.yearField ? await countRecordsByYear(source, currentYear) : await ResourceService.count(source.endpoint, source.params) }))),
          ResourceService.getPage('/milestones', { page: 0, size: 6 }),
        ]);
        if (active) { setStats(counts); setMilestones(milestonePage.content); }
      } catch (requestError) { if (active) setError(requestError); }
      finally { if (active) setLoading(false); }
    };
    load();
    return () => { active = false; };
  }, [section, currentYear]);

  const tasks = useMemo(() => milestones.filter((item) => item.status === 'OPEN' || item.status === 'DRAFT').sort((a, b) => new Date(a.deadline || 0) - new Date(b.deadline || 0)).slice(0, 4), [milestones]);
  const completed = milestones.filter((item) => item.status === 'CLOSED').length;
  const completion = milestones.length ? Math.round((completed / milestones.length) * 100) : 0;
  const [title, subtitle] = sectionCopy[section];

  return <div className="page-stack">
    <section className="welcome-card"><div><span className="welcome-kicker">HỆ THỐNG QUẢN LÝ ĐỒ ÁN TỐT NGHIỆP</span><h2>{title}</h2><p>{subtitle}</p></div><button className="primary-button">Xem kế hoạch <ArrowRight size={17} /></button></section>
    {loading ? <section className="panel dashboard-state"><LoaderCircle className="spin" size={28} /><span>Đang tổng hợp dữ liệu...</span></section> : error ? <section className="panel dashboard-state error"><AlertCircle size={28} /><span>{error.message || 'Không thể tải số liệu dashboard.'}</span></section> : <>
      <section className="stat-grid">{stats.map(({ label, value, Icon, color, yearField }) => <article className="stat-card" key={label}><div className={`stat-icon ${color}`}><Icon size={21} /></div><span>{label}</span><strong>{new Intl.NumberFormat('vi-VN').format(value)}</strong><small>{yearField ? `Dữ liệu trong năm ${currentYear}` : 'Dữ liệu cập nhật từ hệ thống'}</small></article>)}</section>
      <section className="dashboard-grid">
        <article className="panel activity-panel"><div className="panel-heading"><div><span className="panel-kicker">MỐC TIẾN ĐỘ</span><h3>Công việc sắp tới</h3></div></div>{tasks.length ? <div className="task-list">{tasks.map((task, index) => <div className="task-row" key={task.milestoneId}><span className="task-index">{String(index + 1).padStart(2, '0')}</span><div><strong>{task.milestoneName}</strong><small>{deadlineText(task.deadline)}</small></div><span className={`priority ${task.status === 'OPEN' ? 'high' : ''}`}>{formatStatus(task.status)}</span><ArrowRight className="row-arrow" size={17} /></div>)}</div> : <div className="inline-empty">Không có milestone đang chờ xử lý.</div>}</article>
        <article className="panel progress-panel"><div className="panel-heading"><div><span className="panel-kicker">TIẾN ĐỘ</span><h3>Tình trạng milestone</h3></div></div><div className="overall-progress"><strong>{completion}%</strong><span>{completed}/{milestones.length} mốc đã đóng</span><div className="progress-track"><i style={{ width: `${completion}%` }} /></div></div>{tasks[0] && <div className="deadline-note"><Clock3 size={19} /><div><strong>Deadline gần nhất</strong><span>{tasks[0].milestoneName} · {formatDate(tasks[0].deadline)}</span></div></div>}</article>
      </section>
    </>}
  </div>;
}

function formatStatus(value) { return value ? value.replaceAll('_', ' ') : '—'; }
function formatDate(value) { if (!value) return 'Chưa đặt hạn'; const date = new Date(value); return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(date); }
function deadlineText(value) { if (!value) return 'Chưa có deadline'; const diff = new Date(value).getTime() - Date.now(); const days = Math.ceil(diff / 86400000); return days < 0 ? `Đã quá hạn ${Math.abs(days)} ngày` : `Còn ${days} ngày · ${formatDate(value)}`; }
async function countRecordsByYear(source, year) { const records = await ResourceService.getAll(source.endpoint, source.params); return records.filter((record) => { const rawDate = record?.[source.yearField]; if (!rawDate) return false; const parsed = new Date(rawDate); return !Number.isNaN(parsed.getTime()) && parsed.getFullYear() === year; }).length; }
