import React, { useEffect, useMemo, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Bell, BookOpen, CalendarClock, CalendarDays, ChevronDown, ChevronRight, ClipboardCheck,
  FileText, GraduationCap, LayoutDashboard, LogOut, Menu, PanelLeftClose, School,
  Search, Settings, ShieldCheck, Star, Users, X,
} from 'lucide-react';
import logo from '../img/sv_logo_dashboard.png';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import '../style/ApplicationLayout.scss';
import '../style/StudentHeader.scss';

const menus = {
  admin: [
    { label: 'Tổng quan', path: '/admin', icon: LayoutDashboard, end: true },
    { label: 'Sinh viên', path: '/admin/students', icon: GraduationCap },
    { label: 'Giảng viên', path: '/admin/lecturers', icon: Users },
    { label: 'Năm học', path: '/admin/academic-years', icon: CalendarDays },
    { label: 'Đợt bảo vệ', path: '/admin/defense-periods', icon: CalendarClock },
    { label: 'Ngành học', path: '/admin/majors', icon: GraduationCap },
    { label: 'Lớp học', path: '/admin/classes', icon: School },
    { label: 'Nhóm sinh viên', path: '/admin/teams', icon: Users },
    { label: 'Quản lý đề tài', path: '/admin/topics', icon: BookOpen },
    { label: 'Tiến độ đồ án', path: '/admin/progress', icon: ClipboardCheck },
    { label: 'Hội đồng', path: '/admin/committees', icon: ShieldCheck },
    { label: 'Lịch bảo vệ', path: '/admin/schedules', icon: CalendarDays },
    { label: 'Biểu mẫu', path: '/admin/templates', icon: FileText },
    { label: 'Thư viện đề tài', path: '/admin/library-topics', icon: BookOpen },
    { label: 'Phân quyền', path: '/admin/permissions', icon: Settings },
  ],
  lecturer: [
    { label: 'Tổng quan', path: '/lecturer', icon: LayoutDashboard, end: true },
    { label: 'Sinh viên hướng dẫn', path: '/lecturer/supervision', icon: Users },
    { label: 'Duyệt báo cáo', path: '/lecturer/submissions', icon: ClipboardCheck },
    { label: 'Phản biện', path: '/lecturer/reviews', icon: BookOpen },
    { label: 'Lịch hội đồng', path: '/lecturer/schedules', icon: CalendarDays },
    { label: 'Chấm điểm', path: '/lecturer/scores', icon: Star },
  ],
  student: [
    { label: 'Trang chức năng', path: '/student', icon: LayoutDashboard, end: true },
    { label: 'Nhóm của tôi', path: '/student/team', icon: Users },
    { label: 'Đề tài', path: '/student/topics', icon: BookOpen },
    { label: 'Tiến độ & nộp bài', path: '/student/progress', icon: ClipboardCheck },
    { label: 'Lịch bảo vệ', path: '/student/schedule', icon: CalendarDays },
    { label: 'Kết quả', path: '/student/results', icon: Star },
    { label: 'Biểu mẫu', path: '/student/templates', icon: FileText },
    { label: 'Thư viện đề tài', path: '/student/library-topics', icon: BookOpen },
  ],
};

const sectionNames = { admin: 'BAN QUẢN LÝ KHOA', lecturer: 'KHÔNG GIAN GIẢNG VIÊN', student: 'CỔNG SINH VIÊN' };

export default function ApplicationLayout({ section }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [studentMenuOpen, setStudentMenuOpen] = useState(false);
  const { user, logout } = useAuth();
  const toast = useToast();
  const location = useLocation();
  const previousPath = useRef(location.pathname);
  const navigate = useNavigate();
  const homePath = section === 'admin' ? '/admin' : section === 'lecturer' ? '/lecturer' : '/student';
  const active = useMemo(() => menus[section].find((item) => item.end ? location.pathname === item.path : location.pathname.startsWith(item.path)), [location.pathname, section]);
  const profileName = user?.fullName?.trim() || user?.userName?.trim() || 'Đang tải thông tin';

  useEffect(() => {
    if (previousPath.current !== location.pathname) {
      toast.dismissAll();
      previousPath.current = location.pathname;
    }
  }, [location.pathname, toast]);

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className={`application ${collapsed ? 'sidebar-collapsed' : ''} ${section === 'student' ? 'student-application' : ''}`}>
      {section !== 'student' && mobileOpen && <button className="sidebar-backdrop" aria-label="Đóng menu" onClick={() => setMobileOpen(false)} />}
      {section !== 'student' && <aside className={`app-sidebar ${mobileOpen ? 'mobile-open' : ''}`}>
        <div className="brand-row">
          <NavLink className="brand-home-link" to={homePath} aria-label="Về trang chủ">
            <img src={logo} alt="Đại học Công nghiệp Việt – Hung" />
          </NavLink>
          <button className="mobile-close" onClick={() => setMobileOpen(false)}><X size={20} /></button>
        </div>
        <div className="section-label">{sectionNames[section]}</div>
        <nav className="main-nav" aria-label="Điều hướng chính">
          {menus[section].map(({ label, path, icon: Icon, end }) => (
            <NavLink key={path} to={path} end={end} onClick={() => setMobileOpen(false)} title={label}>
              <Icon size={19} /><span>{label}</span><ChevronRight className="nav-chevron" size={15} />
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-help">
          <div className="help-icon"><ShieldCheck size={20} /></div>
          <strong>Cần hỗ trợ?</strong>
          <span>Liên hệ Ban quản lý khoa CNTT</span>
        </div>
        <button className="collapse-button" onClick={() => setCollapsed((value) => !value)}>
          <PanelLeftClose size={18} /><span>Thu gọn menu</span>
        </button>
      </aside>}

      <div className="app-frame">
        <header className="app-header">
          {section === 'student' ? <button className="student-header-logo" onClick={() => navigate('/student')} aria-label="Về trang chức năng"><img src={logo} alt="Đại học Công nghiệp Việt – Hung" /></button> : <div className="header-left"><button className="mobile-menu" onClick={() => setMobileOpen(true)}><Menu size={22} /></button><div><span className="eyebrow">Hệ thống quản lý đồ án</span><h1>{active?.label || 'Quản lý đồ án tốt nghiệp'}</h1></div></div>}
          <div className="header-actions">
            <label className="global-search"><Search size={17} /><input placeholder="Tìm kiếm nhanh..." /></label>
            <button className="icon-button notification-button" aria-label="Thông báo"><Bell size={20} /><span /></button>
            <div className="profile-wrap">
              <button className="profile-button" onClick={() => setProfileOpen((value) => !value)}>
                <span className="avatar">{section === 'student' ? initials(profileName) : section === 'lecturer' ? 'GV' : 'AD'}</span>
                <span className="profile-copy"><strong title={profileName}>{profileName}</strong><small>{sectionNames[section]}</small></span>
                <ChevronDown size={16} />
              </button>
              {profileOpen && <div className="profile-menu"><button onClick={handleLogout}><LogOut size={17} /> Đăng xuất</button></div>}
            </div>
          </div>
        </header>
        {section === 'student' && <div className="student-body-menu"><div className="student-menu-wrap"><button className="student-menu-trigger" onClick={() => setStudentMenuOpen((value) => !value)} aria-expanded={studentMenuOpen}><Menu size={19} /><span>Chức năng</span><ChevronDown size={15} /></button>{studentMenuOpen && <nav className="student-header-menu" aria-label="Chức năng sinh viên">{menus.student.map(({ label, path, icon: Icon, end }) => <NavLink key={path} to={path} end={end} onClick={() => setStudentMenuOpen(false)}><Icon size={18} /><span>{label}</span><ChevronRight size={15} /></NavLink>)}</nav>}</div></div>}
        <main className="app-content"><Outlet /></main>
      </div>
    </div>
  );
}

function initials(value) {
  const parts = String(value).trim().split(/\s+/).filter(Boolean);
  if (!parts.length || value === 'Đang tải thông tin') return 'SV';
  return parts.slice(-2).map((part) => part[0]).join('').toUpperCase();
}
