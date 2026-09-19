import React, { useEffect, useState } from 'react';
import {
  ArrowUpRight,
  Bell,
  BookMarked,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  ClipboardCheck,
  FileDown,
  FolderKanban,
  GraduationCap,
  LibraryBig,
  UserRoundCheck,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import httpClient from '../../config/HttpClient.jsx';
import { API_ENDPOINTS } from '../../config/endpoints.js';
import '../../style/StudentHome.scss';

const actions = [
  { number: '01', title: 'Nhóm của tôi', subtitle: 'Không gian làm việc nhóm', description: 'Thành viên và đề tài đang thực hiện.', path: '/student/team', Icon: FolderKanban, color: 'indigo' },
  { number: '02', title: 'Chọn đề tài', subtitle: 'Đăng ký đồ án', description: 'Lựa chọn hoặc gửi đề xuất đề tài.', path: '/student/topics', Icon: BookMarked, color: 'sky' },
  { number: '03', title: 'Ghi danh đồ án', subtitle: 'Đăng ký đợt bảo vệ', description: 'Theo dõi hồ sơ ghi danh của bạn.', path: '/student/enrollment', Icon: UserRoundCheck, color: 'cyan' },
  { number: '04', title: 'Nộp báo cáo', subtitle: 'Tiến độ thực hiện', description: 'Xem mốc tiến độ và nộp phiên bản báo cáo.', path: '/student/progress', Icon: ClipboardCheck, color: 'amber' },
  { number: '05', title: 'Lịch bảo vệ', subtitle: 'Thông tin buổi bảo vệ', description: 'Thời gian, phòng và hội đồng bảo vệ.', path: '/student/schedule', Icon: CalendarDays, color: 'violet' },
  { number: '06', title: 'Kết quả', subtitle: 'Nhận xét và điểm số', description: 'Xem phản hồi và điểm đã công bố.', path: '/student/results', Icon: CheckCircle2, color: 'emerald' },
  { number: '07', title: 'Tải biểu mẫu', subtitle: 'Tài liệu học phần', description: 'Tải các biểu mẫu cần thiết cho đồ án.', path: '/student/templates', Icon: FileDown, color: 'rose' },
  { number: '08', title: 'Kho đề tài', subtitle: 'Nguồn tham khảo', description: 'Tham khảo các đề tài trong thư viện.', path: '/student/library-topics', Icon: LibraryBig, color: 'teal' },
];

const valueOr = (values, fallback = 'Chưa cập nhật') => {
  const value = values.find((item) => item !== undefined && item !== null && String(item).trim() !== '');
  return value === undefined ? fallback : String(value);
};

export default function StudentHomePage() {
  const { user } = useAuth();
  const [studentProfile, setStudentProfile] = useState(null);

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

      <section className="student-shortcuts-section">
        <div className="student-section-heading"><div><span>TRUY CẬP NHANH</span><h3>Các chức năng đồ án</h3></div><BookOpen size={20} /></div>
        <nav className="student-shortcuts-grid" aria-label="Chức năng đồ án tốt nghiệp">
          {actions.map(({ number, title, subtitle, description, path, Icon, color }) => (
            <Link className={`student-shortcut ${color}`} to={path} key={path}>
              <span className="student-shortcut-number">{number}</span>
              <span className="student-shortcut-icon"><Icon size={21} /></span>
              <span className="student-shortcut-content"><small>{subtitle}</small><strong>{title}</strong><p>{description}</p></span>
              <span className="student-shortcut-footer">Truy cập <ArrowUpRight size={15} /></span>
            </Link>
          ))}
        </nav>
      </section>

      <section className="student-dashboard-bottom">
        <article className="student-panel student-process-card">
          <div className="student-section-heading"><div><span>QUY TRÌNH ĐỒ ÁN</span><h3>Các bước cần thực hiện</h3></div><ClipboardCheck size={20} /></div>
          <div className="student-process-list">
            <ProcessStep number="01" title="Tham gia nhóm" description="Kiểm tra thành viên và đề tài của nhóm." path="/student/team" />
            <ProcessStep number="02" title="Đăng ký đề tài" description="Chọn đề tài có sẵn hoặc gửi đề xuất mới." path="/student/topics" />
            <ProcessStep number="03" title="Theo dõi tiến độ" description="Nộp báo cáo theo từng mốc được yêu cầu." path="/student/progress" />
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

function ProcessStep({ number, title, description, path }) {
  return <Link className="student-process-step" to={path}><span>{number}</span><div><strong>{title}</strong><p>{description}</p></div><ArrowUpRight size={16} /></Link>;
}

function ResourceLink({ path, title, description, Icon }) {
  return <Link className="student-resource-link" to={path}><span><Icon size={18} /></span><div><strong>{title}</strong><p>{description}</p></div><ArrowUpRight size={16} /></Link>;
}

function initials(value) {
  const parts = String(value).trim().split(/\s+/).filter(Boolean);
  return parts.slice(-2).map((part) => part[0]).join('').toUpperCase() || 'SV';
}
