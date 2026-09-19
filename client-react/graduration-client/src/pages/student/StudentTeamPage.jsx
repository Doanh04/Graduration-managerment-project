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
  const [team, setTeam] = useState(null);
  const [selectedTeamId, setSelectedTeamId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadTeam = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await httpClient.get(API_ENDPOINTS.teams.mineAll);
      const payload = normalizeResponse(response);
      const availableTeams = Array.isArray(payload) ? payload : [];
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

  const members = useMemo(() => {
    const uniqueMembers = new Map();
    (Array.isArray(team?.students) ? team.students : []).forEach((member, index) => {
      const key = member?.idStudent || member?.studentCode || `member-${index}`;
      if (!uniqueMembers.has(key)) uniqueMembers.set(key, member);
    });
    return [...uniqueMembers.values()];
  }, [team]);

  const topicAssigned = Boolean(team?.topicId || team?.topicTitle);

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
          {teams.length > 1 && (
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
            <div className="student-team-summary-content">
              <p className="student-team-label">NHÓM SINH VIÊN</p>
              <h2>{team.nameTeam}</h2>
              <p>{team.defensePeriodName ? `${team.defensePeriodName}${team.academicYear ? ` · ${team.academicYear}` : ''}` : valueOrDash(team.description)}</p>
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
              <div className="student-team-stat-icon blue"><Users size={20} /></div>
              <div><span>Thành viên</span><strong>{members.length}</strong></div>
            </article>
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

            <article className="student-team-card student-team-members">
              <div className="student-team-section-heading">
                <Users size={20} />
                <h2>Thành viên nhóm</h2>
                <span className="student-team-count">{members.length}</span>
              </div>
              {members.length ? (
                <div className="student-team-member-list">
                  {members.map((member, index) => {
                    const name = studentName(member);
                    return (
                      <div className="student-team-member" key={member?.idStudent || member?.studentCode || index}>
                        <div className="student-team-avatar">{initials(name)}</div>
                        <div className="student-team-member-info">
                          <strong>{name}</strong>
                          <span>{valueOrDash(member?.studentCode || member?.code)} · {valueOrDash(member?.classCode || member?.class?.classCode)}</span>
                        </div>
                        {member?.emailStudent || member?.email ? (
                          <a href={`mailto:${member.emailStudent || member.email}`} title="Gửi email">
                            <Mail size={17} />
                          </a>
                        ) : <UserRound size={17} className="student-team-member-placeholder" />}
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="student-team-muted">Chưa có thông tin thành viên.</div>
              )}
            </article>
          </section>
        </>
      )}
    </main>
  );
}
