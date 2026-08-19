import React from 'react';
import { ArrowUpRight, BookMarked, CalendarDays, ClipboardCheck, FileDown, FolderKanban, LibraryBig, Star } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import '../../style/StudentHome.scss';

const actions = [
  { number: '01', title: 'Nhóm của tôi', subtitle: 'Không gian làm việc nhóm', description: 'Thành viên, đề tài và kết quả thực hiện riêng của từng sinh viên.', path: '/student/team', Icon: FolderKanban, color: 'indigo' },
  { number: '02', title: 'Chọn đề tài', subtitle: 'Đăng ký đồ án', description: 'Tra cứu danh sách, lựa chọn hoặc gửi đề xuất đề tài cá nhân.', path: '/student/topics', Icon: BookMarked, color: 'sky' },
  { number: '03', title: 'Nộp báo cáo', subtitle: 'Tiến độ thực hiện', description: 'Xem deadline, yêu cầu từng mốc và tải phiên bản báo cáo lên hệ thống.', path: '/student/progress', Icon: ClipboardCheck, color: 'amber', primary: true },
  { number: '04', title: 'Lịch bảo vệ', subtitle: 'Thông tin buổi bảo vệ', description: 'Thời gian, phòng, địa điểm và danh sách thành viên hội đồng.', path: '/student/schedule', Icon: CalendarDays, color: 'violet' },
  { number: '05', title: 'Kết quả', subtitle: 'Nhận xét và điểm số', description: 'Xem phản hồi của giảng viên và điểm đã được công bố.', path: '/student/results', Icon: Star, color: 'emerald' },
  { number: '06', title: 'Tải biểu mẫu', subtitle: 'Tài liệu học phần', description: 'Phiếu đăng ký, nhật ký, mẫu báo cáo và các tài liệu cần thiết.', path: '/student/templates', Icon: FileDown, color: 'rose' },
  { number: '07', title: 'Kho đề tài', subtitle: 'Nguồn tham khảo', description: 'Khám phá ý tưởng, mục tiêu và công nghệ từ thư viện đề tài.', path: '/student/library-topics', Icon: LibraryBig, color: 'teal' },
];

export default function StudentHomePage() {
  const { user } = useAuth();
  const displayName = user?.fullName || user?.userName || 'Sinh viên';

  return <main className="student-workspace">
    <header className="student-workspace-header">
      <div><span>KHÔNG GIAN HỌC PHẦN</span><h2>Đồ án tốt nghiệp</h2><p>Chào {displayName}. Chọn công việc bạn cần thực hiện.</p></div>
      <div className="student-identity"><i>{initials(displayName)}</i><span><small>TÀI KHOẢN SINH VIÊN</small><strong>{displayName}</strong></span></div>
    </header>

    <nav className="student-action-grid" aria-label="Chức năng đồ án tốt nghiệp">
      {actions.map(({ number, title, subtitle, description, path, Icon, color, primary }) => <Link className={`student-action ${color}${primary ? ' primary' : ''}`} to={path} key={path}>
        <span className="student-action-number">{number}</span>
        <span className="student-action-icon"><Icon size={25} /></span>
        <span className="student-action-content"><small>{subtitle}</small><strong>{title}</strong><p>{description}</p></span>
        <span className="student-action-open">Truy cập <ArrowUpRight size={18} /></span>
      </Link>)}
    </nav>
  </main>;
}

function initials(value) {
  const parts = String(value).trim().split(/\s+/);
  return parts.slice(-2).map((part) => part[0]).join('').toUpperCase();
}
