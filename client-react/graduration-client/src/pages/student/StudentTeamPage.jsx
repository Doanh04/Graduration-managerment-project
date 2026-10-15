import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Mail,
  RefreshCw,
  ShieldCheck,
  UserRound,
  Users,
} from 'lucide-react';
import httpClient from '../../config/HttpClient';
import API_ENDPOINTS from '../../config/endpoints';
import '../../style/StudentTeam.scss';

const valueOrDash = (value) => value || 'Chưa cập nhật';

const formatDate = (value) => {
  if (!value) return 'Chưa cập nhật';
  if (typeof value === 'string') {
    const iso = value.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (iso) return `${iso[3]}/${iso[2]}/${iso[1]}`;
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleDateString('vi-VN');
};

const initials = (name) => {
  const words = String(name || '?').trim().split(/\s+/).filter(Boolean);
  if (!words.length) return '?';
  return words.slice(-2).map((word) => word[0]).join('').toUpperCase();
};

const studentName = (student) => (
  student?.fullName
  || student?.fullNameStudent
  || student?.name
  || student?.userName
  || 'Sinh viên'
);

const normalizeResponse = (response) => (
  response?.result
  ?? response?.data?.result
  ?? response?.data
  ?? response
);

export default function StudentTeamPage() {
  const [teams, setTeams] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [schedule, setSchedule] = useState(null);
  const [team, setTeam] = useState(null);
  const [selectedTeamId, setSelectedTeamId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadTeam = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [response, enrollmentResponse] = await Promise.all([
        httpClient.get(API_ENDPOINTS.teams.mineAll),
        httpClient.get(API_ENDPOINTS.graduationEnrollments.mine).catch(() => ({ result: [] })),
      ]);
      const payload = normalizeResponse(response);
      const enrollmentPayload = normalizeResponse(enrollmentResponse);
      const availableTeams = Array.isArray(payload) ? payload : [];
      setEnrollments(Array.isArray(enrollmentPayload) ? enrollmentPayload : []);
      setTeams(availableTeams);
      setSelectedTeamId((current) => {
        const selected = availableTeams.find((item) => String(item.idTeam) === String(current));
        return selected ? String(selected.idTeam) : String(availableTeams[0]?.idTeam || '');
      });
    } catch (requestError) {
      setTeams([]);
      setSelectedTeamId('');
      setError(
        requestError?.response?.data?.message
        || requestError?.response?.data?.error
        || 'Không thể tải thông tin nhóm của bạn. Vui lòng thử lại.',
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTeam();
  }, [loadTeam]);

  useEffect(() => {
    setTeam(teams.find((item) => String(item.idTeam) === String(selectedTeamId)) || null);
  }, [teams, selectedTeamId]);

  useEffect(() => {
    let cancelled = false;
    setSchedule(null);
    if (!team?.defensePeriodId) return undefined;
    httpClient.get(`/defense-periods/${team.defensePeriodId}/schedules`, { params: { page: 0, size: 100 } })
      .then((response) => {
        const result = normalizeResponse(response);
        const items = Array.isArray(result) ? result : (Array.isArray(result?.content) ? result.content : []);
        const current = items.find((item) => String(item.teamId) === String(team.idTeam) || String(item.topicId) === String(team.topicId));
        if (!cancelled) setSchedule(current || null);
      }).catch(() => { if (!cancelled) setSchedule(null); });
    return () => { cancelled = true; };
  }, [team]);

  const topicAssigned = Boolean(team?.topicId || team?.topicTitle);
  const currentEnrollment = enrollments.find((enrollment) => (
    String(enrollment?.defensePeriodId || '') === String(team?.defensePeriodId || '')
  ));
  const defenseBanned = String(currentEnrollment?.status || '').toUpperCase() === 'WITHDRAWN';

  return (
    <main className="student-team-page">
      <section className="student-team-hero">
        <div>
          <p className="student-team-eyebrow">QUẢN LÝ DỮ LIỆU</p>
          <h1>Nhóm của tôi</h1>
          <p className="student-team-subtitle">
            Thông tin thành viên và đề tài đang thực hiện của nhóm.
          </p>
        </div>
        <button
          type="button"
          className="student-team-refresh"
          onClick={loadTeam}
          disabled={loading}
        >
          <RefreshCw size={16} className={loading ? 'is-spinning' : ''} />
          Làm mới
        </button>
      </section>

      {loading && (
        <section className="student-team-state student-team-loading" aria-live="polite">
          <RefreshCw size={28} className="is-spinning" />
          <p>Đang tải thông tin nhóm...</p>
        </section>
      )}

      {!loading && error && (
        <section className="student-team-state student-team-error" role="alert">
          <AlertCircle size={28} />
          <h2>Không thể tải dữ liệu</h2>
          <p>{error}</p>
          <button type="button" onClick={loadTeam}>Thử lại</button>
        </section>
      )}

      {!loading && !error && !team && (
        <section className="student-team-state">
          <Users size={34} />
          <h2>Bạn chưa thuộc nhóm sinh viên nào</h2>
          <p>Khi được thêm vào một nhóm, thông tin nhóm sẽ hiển thị tại đây.</p>
          <button type="button" onClick={loadTeam}>Làm mới</button>
        </section>
      )}

      {!loading && !error && team && (
        <>
          {teams.length > 0 && (
            <section className="student-team-picker student-team-card">
              <div>
                <span>NHÓM / ĐỢT BẢO VỆ</span>
                <strong>Chọn nhóm cần xem</strong>
              </div>
              <select value={selectedTeamId} onChange={(event) => setSelectedTeamId(event.target.value)}>
                {teams.map((item) => (
                  <option key={item.idTeam} value={item.idTeam}>
                    {item.nameTeam} · {item.defensePeriodName || 'Chưa gắn đợt'}{item.academicYear ? ` · ${item.academicYear}` : ''}
                  </option>
                ))}
              </select>
            </section>
          )}
          <section className="student-team-summary student-team-card">
            <div className="student-team-summary-icon"><Users size={25} /></div>
            <div className={`student-team-summary-content ${defenseBanned ? 'defense-ineligible-student' : ''}`}>
              <p className="student-team-label">NHÓM SINH VIÊN</p>
              <h2>{team.nameTeam}</h2>
              <p>{team.defensePeriodName ? `${team.defensePeriodName}${team.academicYear ? ` · ${team.academicYear}` : ''}` : valueOrDash(team.description)}</p>
              {defenseBanned && <em className="defense-ineligible-badge">Sinh viên bị cấm thi</em>}
            </div>
            <div className="student-team-summary-meta">
              <span><ShieldCheck size={15} /> Vai trò</span>
              <strong>{valueOrDash(team.role)}</strong>
              <span><CalendarDays size={15} /> Ngày tham gia</span>
              <strong>{formatDate(team.joinDate)}</strong>
            </div>
          </section>

          <section className="student-team-stats">
            <article className="student-team-stat student-team-card">
              <div className="student-team-stat-icon green"><CheckCircle2 size={20} /></div>
              <div><span>Đề tài</span><strong>{topicAssigned ? 'Đã gán' : 'Chưa gán'}</strong></div>
            </article>
            <article className="student-team-stat student-team-card">
              <div className="student-team-stat-icon purple"><ShieldCheck size={20} /></div>
              <div><span>Mã nhóm</span><strong>{valueOrDash(team.idTeam)}</strong></div>
            </article>
          </section>

          <section className="student-team-grid">
            <article className="student-team-card student-team-topic">
              <div className="student-team-section-heading">
                <BookOpen size={20} />
                <h2>Đề tài thực hiện</h2>
              </div>
              {topicAssigned ? (
                <>
                  <h3>{team.topicTitle}</h3>
                  <p>{valueOrDash(team.topicDescription)}</p>
                </>
              ) : (
                <div className="student-team-muted">Nhóm chưa được gán đề tài.</div>
              )}
            </article>
            <article className="student-team-card student-team-topic">
              <div className="student-team-section-heading"><CalendarDays size={20} /><h2>Lịch bảo vệ</h2></div>
              {schedule ? <div className="student-team-schedule-info"><strong>{formatDate(schedule.defenseDate)} · {schedule.startTime || '—'} – {schedule.endTime || '—'}</strong><span>Phòng {schedule.room || '—'} · {schedule.location || '—'}</span><span>Hội đồng: {schedule.committeeName || '—'}</span>{Array.isArray(schedule.committeeMembers) && schedule.committeeMembers.length > 0 && <small>Giảng viên: {schedule.committeeMembers.map((member) => member.lectureName || member.lectureCode).filter(Boolean).join(', ')}</small>}</div> : <div className="student-team-muted">Chưa có lịch bảo vệ.</div>}
            </article>

            <article className="student-team-card student-team-members">
              <div className="student-team-section-heading">
                <UserRound size={20} />
                <h2>Thông tin của tôi</h2>
              </div>
              <div className={`student-team-member ${defenseBanned ? 'defense-ineligible-student' : ''}`}>
                <div className="student-team-avatar">{initials(currentEnrollment?.studentName || 'Bạn')}</div>
                <div className="student-team-member-info">
                  <strong>{currentEnrollment?.studentName || 'Bạn'}{defenseBanned && <em className="defense-ineligible-badge">Sinh viên bị cấm thi</em>}</strong>
                  <span>{valueOrDash(currentEnrollment?.studentCode)}</span>
                </div>
              </div>
            </article>
          </section>
        </>
      )}
    </main>
  );
}
