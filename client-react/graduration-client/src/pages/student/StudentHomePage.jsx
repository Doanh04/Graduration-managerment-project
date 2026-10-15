import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowUpRight,
  Bell,
  BookMarked,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Circle,
  ClipboardCheck,
  Clock3,
  FileDown,
  FolderKanban,
  GraduationCap,
  LibraryBig,
  LockKeyhole,
  LoaderCircle,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import httpClient from '../../config/HttpClient.jsx';
import { API_ENDPOINTS } from '../../config/endpoints.js';
import ResourceService from '../../services/ResourceService.jsx';
import { formatDateTimeDisplay } from '../../utils/dateFormat.js';
import '../../style/StudentHome.scss';
import '../../style/StudentHomeSettings.scss';

const actions = [
  { number: 'BV', title: 'Hồ sơ phản biện', subtitle: 'Điều kiện xếp lịch', description: 'Nộp báo cáo và link source code / sản phẩm trước hạn phản biện.', path: '/student/defense-reports', Icon: ClipboardCheck, color: 'sky' },
  { number: '01', title: 'Nhóm của tôi', subtitle: 'Không gian làm việc nhóm', description: 'Thành viên và đề tài đang thực hiện.', path: '/student/team', Icon: FolderKanban, color: 'indigo' },
  { number: '02', title: 'Chọn đề tài', subtitle: 'Đăng ký đồ án', description: 'Lựa chọn hoặc gửi đề xuất đề tài.', path: '/student/topics', Icon: BookMarked, color: 'sky' },
  { number: '03', title: 'Nộp báo cáo', subtitle: 'Tiến độ thực hiện', description: 'Xem mốc tiến độ và nộp phiên bản báo cáo.', path: '/student/progress', Icon: ClipboardCheck, color: 'amber' },
  { number: '04', title: 'Lịch bảo vệ', subtitle: 'Thông tin buổi bảo vệ', description: 'Thời gian, phòng và hội đồng bảo vệ.', path: '/student/schedule', Icon: CalendarDays, color: 'violet' },
  { number: '05', title: 'Kết quả', subtitle: 'Nhận xét và điểm số', description: 'Xem phản hồi và điểm đã công bố.', path: '/student/results', Icon: CheckCircle2, color: 'emerald' },
  { number: '06', title: 'Tải biểu mẫu', subtitle: 'Tài liệu học phần', description: 'Tải các biểu mẫu cần thiết cho đồ án.', path: '/student/templates', Icon: FileDown, color: 'rose' },
  { number: '07', title: 'Kho đề tài', subtitle: 'Nguồn tham khảo', description: 'Tham khảo các đề tài trong thư viện.', path: '/student/library-topics', Icon: LibraryBig, color: 'teal' },
];

const valueOr = (values, fallback = 'Chưa cập nhật') => {
  const value = values.find((item) => item !== undefined && item !== null && String(item).trim() !== '');
  return value === undefined ? fallback : String(value);
};

export default function StudentHomePage() {
  const { user } = useAuth();
  const [studentProfile, setStudentProfile] = useState(null);
  const [resourceCounts, setResourceCounts] = useState({ libraryTopics: 0, templates: 0 });
  const [projectTeams, setProjectTeams] = useState([]);
  const [selectedPeriodId, setSelectedPeriodId] = useState('');
  const [projectLoading, setProjectLoading] = useState(true);
  const [projectMilestones, setProjectMilestones] = useState([]);
  const [projectSubmissions, setProjectSubmissions] = useState([]);
  const [progressLoading, setProgressLoading] = useState(false);
  const [systemSettings, setSystemSettings] = useState({ studentTopicProposalEnabled: false });

  useEffect(() => {
    let cancelled = false;

    const loadProfile = async () => {
      try {
        const response = await httpClient.get(API_ENDPOINTS.students.current);
        const payload = response?.result ?? response?.data?.result ?? response?.data ?? response;
        if (!cancelled) {
          setStudentProfile(payload);
        }
      } catch {
        // Giữ dữ liệu từ AuthContext nếu endpoint hồ sơ chưa sẵn sàng.
      }
    };

    loadProfile();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    const loadStudentOverview = async () => {
      setProjectLoading(true);
      const [libraryCount, templateCount, teams, periods, currentSettings] = await Promise.all([
        ResourceService.count(API_ENDPOINTS.libraryTopics.list).catch(() => 0),
        ResourceService.count(API_ENDPOINTS.templates.list).catch(() => 0),
        ResourceService.getAll(API_ENDPOINTS.teams.mineAll).catch(() => []),
        ResourceService.getAll(API_ENDPOINTS.defensePeriods.list).catch(() => []),
        ResourceService.getOne(API_ENDPOINTS.systemSettings.current).catch(() => ({ studentTopicProposalEnabled: false })),
      ]);
      if (cancelled) return;
      const periodMap = new Map(periods.map((period) => [String(period.defensePeriodId), period]));
      const eligibleByPeriod = new Map();
      teams
        .filter((team) => team?.defensePeriodId && (team?.topicId || team?.topicTitle))
        .forEach((team) => {
          const periodId = String(team.defensePeriodId);
          if (!eligibleByPeriod.has(periodId)) eligibleByPeriod.set(periodId, { ...team, period: periodMap.get(periodId) });
        });
      const eligibleTeams = [...eligibleByPeriod.values()].sort(compareProjectPeriodsNewestFirst);
      setResourceCounts({ libraryTopics: libraryCount, templates: templateCount });
      setSystemSettings((current) => ({ ...current, ...currentSettings }));
      setProjectTeams(eligibleTeams);
      setSelectedPeriodId((current) => eligibleTeams.some((team) => String(team.defensePeriodId) === current)
        ? current
        : String(eligibleTeams[0]?.defensePeriodId ?? ''));
      setProjectLoading(false);
    };
    loadStudentOverview();
    return () => { cancelled = true; };
  }, []);

  const studentData = studentProfile || {};
  const displayName = valueOr([studentData.fullName, user?.fullName, user?.fullNameStudent, user?.userName], 'Sinh viên');
  const profile = {
    studentCode: valueOr([studentData.studentCode, user?.studentCode, user?.idStudent, user?.studentId, user?.code]),
    email: valueOr([studentData.email, user?.email, user?.emailStudent]),
    phone: valueOr([studentData.phone, user?.phone, user?.phoneStudent]),
    classCode: valueOr([
      studentData.classCode,
      studentData.className,
      studentData.class?.classCode,
      studentData.class?.className,
      user?.classCode,
      user?.className,
      user?.class?.classCode,
      user?.class?.className,
    ]),
    major: valueOr([
      studentData.majorName,
      studentData.major?.majorName,
      studentData.class?.major?.majorName,
      user?.majorName,
      user?.major?.majorName,
      user?.major,
    ]),
  };
  const selectedProjectTeam = useMemo(() => projectTeams.find((team) => String(team.defensePeriodId) === selectedPeriodId) || null, [projectTeams, selectedPeriodId]);

  useEffect(() => {
    let cancelled = false;
    if (!selectedProjectTeam) {
      setProjectMilestones([]);
      setProjectSubmissions([]);
      return undefined;
    }
    setProjectMilestones([]);
    setProjectSubmissions([]);
    setProgressLoading(true);
    Promise.all([
      ResourceService.getAll(API_ENDPOINTS.milestones.list, { defensePeriodId: selectedProjectTeam.defensePeriodId }),
      ResourceService.getAll(API_ENDPOINTS.submissions.byTeam(selectedProjectTeam.idTeam)),
    ]).then(([milestones, submissions]) => {
      if (cancelled) return;
      setProjectMilestones(milestones
        .filter((milestone) => milestone.required !== false && String(milestone.status || '').toUpperCase() !== 'CANCELLED')
        .sort((left, right) => new Date(left.deadline || 0) - new Date(right.deadline || 0)));
      setProjectSubmissions(submissions);
    }).catch(() => {
      if (!cancelled) {
        setProjectMilestones([]);
        setProjectSubmissions([]);
      }
    }).finally(() => { if (!cancelled) setProgressLoading(false); });
    return () => { cancelled = true; };
  }, [selectedProjectTeam]);

  return (
    <main className="student-dashboard">
      <header className="student-dashboard-heading">
        <div>
          <span className="student-dashboard-eyebrow">KHÔNG GIAN HỌC PHẦN</span>
          <h2>Đồ án tốt nghiệp</h2>
          <p>Chào {displayName}. Chọn công việc bạn cần thực hiện.</p>
        </div>
        <div className="student-identity">
          <i>{initials(displayName)}</i>
          <span><small>TÀI KHOẢN SINH VIÊN</small><strong>{displayName}</strong></span>
        </div>
      </header>

      <section className="student-overview-grid">
        <article className="student-panel student-profile-card">
          <div className="student-panel-heading">
            <span className="student-panel-icon indigo"><GraduationCap size={19} /></span>
            <div><small>THÔNG TIN TÀI KHOẢN</small><h3>Thông tin sinh viên</h3></div>
          </div>
          <div className="student-profile-summary">
            <div className="student-profile-avatar">{initials(displayName)}</div>
            <div><strong>{displayName}</strong><span>{profile.studentCode} · Sinh viên</span></div>
          </div>
          <dl className="student-profile-fields">
            <ProfileField label="MSSV" value={profile.studentCode} />
            <ProfileField label="Họ và tên" value={displayName} />
            <ProfileField label="Email" value={profile.email} />
            <ProfileField label="Số điện thoại" value={profile.phone} />
            <ProfileField label="Lớp học" value={profile.classCode} />
            <ProfileField label="Ngành học" value={profile.major} />
          </dl>
        </article>

        <aside className="student-status-column">
          <article className="student-panel student-notice-card">
            <div className="student-notice-icon"><Bell size={20} /></div>
            <small>THÔNG BÁO HỌC PHẦN</small>
            <strong>Không có thông báo mới</strong>
            <p>Các cập nhật về tiến độ, lịch bảo vệ và kết quả sẽ hiển thị tại đây.</p>
            <Link to="/student/progress">Xem tiến độ <ArrowUpRight size={15} /></Link>
          </article>
          <div className="student-mini-status-grid">
            <Link className="student-mini-status cyan" to="/student/progress">
              <ClipboardCheck size={20} /><small>TIẾN ĐỘ ĐỒ ÁN</small><strong>Nộp báo cáo</strong><span>Theo dõi mốc tiến độ</span>
            </Link>
            <Link className="student-mini-status violet" to="/student/schedule">
              <CalendarDays size={20} /><small>LỊCH BẢO VỆ</small><strong>Lịch bảo vệ</strong><span>Xem lịch và hội đồng</span>
            </Link>
          </div>
        </aside>
      </section>

      <section className="student-project-overview">
        <div className="student-resource-stats">
          <Link className="student-resource-stat library" to="/student/library-topics">
            <span><LibraryBig size={22} /></span>
            <div><small>THƯ VIỆN ĐỀ TÀI</small><strong>{new Intl.NumberFormat('vi-VN').format(resourceCounts.libraryTopics)}</strong><p>Đề tài tham khảo hiện có</p></div>
            <ArrowUpRight size={16} />
          </Link>
          <Link className="student-resource-stat template" to="/student/templates">
            <span><FileDown size={22} /></span>
            <div><small>BIỂU MẪU HỌC PHẦN</small><strong>{new Intl.NumberFormat('vi-VN').format(resourceCounts.templates)}</strong><p>Biểu mẫu có thể sử dụng</p></div>
            <ArrowUpRight size={16} />
          </Link>
        </div>

        <article className="student-panel student-project-card">
          <header>
            <div><span>ĐỀ TÀI & NHÓM SINH VIÊN</span><h3>Thông tin đồ án đang thực hiện</h3></div>
            <label>
              <span>Đợt bảo vệ</span>
              <select value={selectedPeriodId} onChange={(event) => setSelectedPeriodId(event.target.value)} disabled={projectLoading || !projectTeams.length}>
                {!projectTeams.length && <option value="">Chưa có đợt đủ thông tin</option>}
                {projectTeams.map((team) => <option key={team.defensePeriodId} value={team.defensePeriodId}>{team.defensePeriodName || team.period?.periodName || `Đợt #${team.defensePeriodId}`}{team.academicYear || team.period?.academicYear ? ` · ${team.academicYear || team.period?.academicYear}` : ''}</option>)}
              </select>
            </label>
          </header>
          {projectLoading ? <div className="student-project-state"><LoaderCircle className="spin" size={24} /> Đang tải thông tin đề tài...</div>
            : selectedProjectTeam ? <StudentProjectDetail team={selectedProjectTeam} milestones={projectMilestones} submissions={projectSubmissions} progressLoading={progressLoading} />
              : <div className="student-project-state"><BookOpen size={25} /> Chỉ hiển thị khi bạn đã có nhóm và nhóm đã được gán đề tài trong một đợt bảo vệ.</div>}
        </article>
      </section>

      <section className="student-shortcuts-section">
        <div className="student-section-heading"><div><span>TRUY CẬP NHANH</span><h3>Các chức năng đồ án</h3></div><BookOpen size={20} /></div>
        <nav className="student-shortcuts-grid" aria-label="Chức năng đồ án tốt nghiệp">
          {actions.map(({ number, title, subtitle, description, path, Icon, color }) => {
            const disabled = path === '/student/topics' && !systemSettings.studentTopicProposalEnabled;
            const content = <>
              <span className="student-shortcut-number">{number}</span>
              <span className="student-shortcut-icon"><Icon size={21} /></span>
              <span className="student-shortcut-content"><small>{subtitle}</small><strong>{title}</strong><p>{description}</p></span>
              <span className="student-shortcut-footer">{disabled ? <>Đang tắt <LockKeyhole size={14}/></> : <>Truy cập <ArrowUpRight size={15}/></>}</span>
            </>;
            return disabled
              ? <div className={`student-shortcut ${color} disabled`} aria-disabled="true" title="Ban quản lý khoa đang tắt chức năng đề xuất đề tài" key={path}>{content}</div>
              : <Link className={`student-shortcut ${color}`} to={path} key={path}>{content}</Link>;
          })}
        </nav>
      </section>

      <section className="student-dashboard-bottom">
        <article className="student-panel student-process-card">
          <div className="student-section-heading"><div><span>QUY TRÌNH ĐỒ ÁN</span><h3>Các bước cần thực hiện</h3></div><ClipboardCheck size={20} /></div>
          <div className="student-process-list" role="list" aria-label="Toàn bộ quy trình thực hiện đồ án tốt nghiệp">
            <ProcessStep number="01" title="Tham gia nhóm" description="Tham gia nhóm sinh viên của đợt bảo vệ và kiểm tra thông tin các thành viên." />
            <ProcessStep number="02" title="Đăng ký đề tài" description="Chọn đề tài có sẵn hoặc gửi đề xuất đề tài mới theo thời gian nhà trường quy định." />
            <ProcessStep number="03" title="Duyệt đề tài và phân công hướng dẫn" description="Chờ đề tài được phê duyệt và giảng viên hướng dẫn được phân công cho nhóm." />
            <ProcessStep number="04" title="Thực hiện đồ án" description="Trao đổi với giảng viên hướng dẫn, hoàn thiện nội dung và sản phẩm của đề tài." />
            <ProcessStep number="05" title="Nộp các mốc tiến độ" description="Theo dõi hạn và nộp báo cáo theo từng mốc; nộp lại khi giảng viên yêu cầu chỉnh sửa." />
            <ProcessStep number="06" title="Nhận lịch bảo vệ" description="Kiểm tra ngày, giờ, phòng, địa điểm và thành viên hội đồng của buổi bảo vệ." />
            <ProcessStep number="07" title="Nộp hồ sơ trước bảo vệ" description="Nộp báo cáo PDF hoặc DOCX cùng link source code hoặc link sản phẩm trước giờ bảo vệ." />
            <ProcessStep number="08" title="Giảng viên phản biện kiểm tra" description="Theo dõi kết quả duyệt và nộp lại khi được yêu cầu; hồ sơ chưa được đồng ý trước giờ bảo vệ sẽ không đủ điều kiện bảo vệ." />
            <ProcessStep number="09" title="Thực hiện bảo vệ" description="Bảo vệ đề tài khi hồ sơ đã được phản biện đồng ý và đến đúng thời gian trong lịch." />
            <ProcessStep number="10" title="Nhận kết quả" description="Xem điểm số và nhận xét sau khi kết quả bảo vệ được công bố." />
          </div>
        </article>
        <article className="student-panel student-resource-card">
          <div className="student-section-heading"><div><span>TÀI NGUYÊN HỌC PHẦN</span><h3>Tài liệu & tra cứu</h3></div><LibraryBig size={20} /></div>
          <div className="student-resource-list">
            <ResourceLink path="/student/templates" title="Biểu mẫu" description="Phiếu đăng ký, nhật ký và mẫu báo cáo." Icon={FileDown} />
            <ResourceLink path="/student/library-topics" title="Kho đề tài" description="Nguồn tham khảo cho ý tưởng đồ án." Icon={LibraryBig} />
          </div>
        </article>
      </section>
    </main>
  );
}

function ProfileField({ label, value }) {
  return <div><dt>{label}</dt><dd>{value}</dd></div>;
}

function ProcessStep({ number, title, description }) {
  return <div className="student-process-step" role="listitem"><span>{number}</span><div><strong>{title}</strong><p>{description}</p></div></div>;
}

function ResourceLink({ path, title, description, Icon }) {
  return <Link className="student-resource-link" to={path}><span><Icon size={18} /></span><div><strong>{title}</strong><p>{description}</p></div><ArrowUpRight size={16} /></Link>;
}

function StudentProjectDetail({ team, milestones, submissions, progressLoading }) {
  const submissionByMilestone = new Map();
  submissions.forEach((submission) => {
    const key = String(submission.milestoneId);
    const current = submissionByMilestone.get(key);
    if (!current || new Date(submission.updatedAt || submission.submittedAt || 0) > new Date(current.updatedAt || current.submittedAt || 0)) submissionByMilestone.set(key, submission);
  });
  const submittedCount = milestones.filter((milestone) => isMilestoneSubmitted(submissionByMilestone.get(String(milestone.milestoneId)))).length;
  return <div className="student-project-detail">
    <section className="student-topic-detail">
      <span className="student-topic-icon"><BookOpen size={22} /></span>
      <div><small>ĐỀ TÀI THỰC HIỆN</small><h4>{team.topicTitle}</h4><p>{team.topicDescription || 'Đề tài chưa cập nhật mô tả chi tiết.'}</p></div>
      <dl>
        <div><dt>Nhóm</dt><dd>{team.nameTeam || 'Chưa cập nhật'}</dd></div>
        <div><dt>Loại đồ án</dt><dd>{team.projectType || 'Chưa cập nhật'}</dd></div>
        <div><dt>Mã đề tài</dt><dd>{team.topicId}</dd></div>
        <div><dt>Đợt bảo vệ</dt><dd>{team.defensePeriodName || team.period?.periodName || 'Chưa cập nhật'}</dd></div>
      </dl>
    </section>
    <section className="student-project-milestones">
      <div className="student-project-milestones-heading"><ClipboardCheck size={18} /><div><small>MỐC TIẾN ĐỘ CẦN NỘP</small><strong>Tiến độ của nhóm</strong></div><span>{submittedCount}/{milestones.length} đã nộp</span></div>
      {progressLoading ? <div className="student-milestone-empty"><LoaderCircle className="spin" size={20} /> Đang tải mốc tiến độ...</div>
        : milestones.length ? <div className="student-milestone-list">{milestones.map((milestone) => {
          const submission = submissionByMilestone.get(String(milestone.milestoneId));
          const submitted = isMilestoneSubmitted(submission);
          return <div className={submitted ? 'submitted' : ''} key={milestone.milestoneId}>
            <span>{submitted ? <CheckCircle2 size={18} /> : submission ? <Clock3 size={18} /> : <Circle size={18} />}</span>
            <div><strong>{milestone.milestoneName}</strong><small>{milestone.deadline ? `Hạn nộp: ${formatDateTimeDisplay(milestone.deadline)}` : 'Chưa đặt hạn nộp'}</small></div>
            <em>{submitted ? 'Đã nộp' : submission ? 'Cần nộp lại' : 'Chưa nộp'}</em>
          </div>;
        })}</div> : <div className="student-milestone-empty">Chưa có mốc tiến độ bắt buộc trong đợt này.</div>}
    </section>
  </div>;
}

function isMilestoneSubmitted(submission) {
  if (!submission) return false;
  return !['WITHDRAWN', 'REVISION_REQUIRED'].includes(String(submission.status || '').toUpperCase());
}

function compareProjectPeriodsNewestFirst(left, right) {
  const leftDate = new Date(left.period?.startDate || left.period?.endDate || 0).getTime();
  const rightDate = new Date(right.period?.startDate || right.period?.endDate || 0).getTime();
  return rightDate - leftDate || Number(right.defensePeriodId || 0) - Number(left.defensePeriodId || 0);
}

function initials(value) {
  const parts = String(value).trim().split(/\s+/).filter(Boolean);
  return parts.slice(-2).map((part) => part[0]).join('').toUpperCase() || 'SV';
}
