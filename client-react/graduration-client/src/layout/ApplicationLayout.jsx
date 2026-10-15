import React, { useEffect, useMemo, useRef, useState } from 'react';
import { hasRole } from '../utils/roles.js';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Archive, BookOpen, CalendarClock, CalendarDays, ChevronDown, ChevronRight, ClipboardCheck,
  Eye, EyeOff, FileCheck2, FileText, GraduationCap, KeyRound, LayoutDashboard, LogOut, Menu, PanelLeftClose, School,
  Search, Settings, ShieldCheck, Star, UserRoundCheck, Users, X, ScrollText,
} from 'lucide-react';
import logo from '../img/sv_logo_dashboard.png';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import PageLoadingSkeleton from '../components/feedback/PageLoadingSkeleton.jsx';
import NotificationCenter from '../components/NotificationCenter.jsx';
import httpClient, { getPendingHttpRequests } from '../config/HttpClient.jsx';
import API_ENDPOINTS from '../config/endpoints.js';
import getApiErrorMessage from '../utils/apiError.js';
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
    { label: 'Phân công hướng dẫn', path: '/admin/supervisor-assignments', icon: UserRoundCheck },
    { label: 'Tiến độ đồ án', path: '/admin/progress', icon: ClipboardCheck },
    { label: 'Hội đồng', path: '/admin/committees', icon: ShieldCheck },
    { label: 'Lịch bảo vệ', path: '/admin/schedules', icon: CalendarDays },
    { label: 'Biểu mẫu', path: '/admin/templates', icon: FileText },
    { label: 'Thư viện đề tài', path: '/admin/library-topics', icon: BookOpen },
    { label: 'Kho lịch sử', path: '/admin/project-history', icon: Archive },
    { label: 'Phân quyền', path: '/admin/permissions', icon: Settings },
    { label: 'Nhật ký hoạt động', path: '/admin/audit-logs', icon: ScrollText, allowedRoles: ['ADMIN'] },
    { label: 'Bài nộp & nhận xét', path: '/admin/submissions', icon: FileText },
    { label: 'Duyệt đăng ký đề tài', path: '/admin/topic-registrations', icon: ClipboardCheck },
    { label: 'Ghi danh đồ án', path: '/admin/graduation-enrollments', icon: UserRoundCheck },
    { label: 'Cài đặt chung', path: '/admin/system-settings', icon: Settings, allowedRoles: ['ADMIN'] },
    { label: 'Duyệt báo cáo', path: '/admin/final-submissions', icon: FileCheck2, allowedRoles: ['ADMIN'] },
  ],
  lecturer: [
    { label: 'Tổng quan', path: '/lecturer', icon: LayoutDashboard, end: true },
    { label: 'Sinh viên hướng dẫn', path: '/lecturer/supervision', icon: Users },
    { label: 'Tiến độ đồ án', path: '/lecturer/submissions', icon: ClipboardCheck },
    { label: 'Phản biện', path: '/lecturer/reviews', icon: BookOpen },
    { label: 'Bảo vệ đồ án', path: '/lecturer/schedules', icon: CalendarDays },
    { label: 'Chấm điểm', path: '/lecturer/scores', icon: Star },
    { label: 'Biểu mẫu', path: '/lecturer/templates', icon: FileText, visibilitySetting: 'templatesVisible' },
    { label: 'Thư viện đề tài', path: '/lecturer/library-topics', icon: BookOpen, visibilitySetting: 'libraryTopicsVisible' },
  ],
  student: [
    { label: 'Trang chức năng', path: '/student', icon: LayoutDashboard, end: true },
    { label: 'Nhóm của tôi', path: '/student/team', icon: Users },
    { label: 'Đề tài', path: '/student/topics', icon: BookOpen },
    { label: 'Tiến độ & nộp bài', path: '/student/progress', icon: ClipboardCheck },
    { label: 'Lịch bảo vệ', path: '/student/schedule', icon: CalendarDays },
    { label: 'Hồ sơ phản biện', path: '/student/defense-reports', icon: ClipboardCheck },
    { label: 'Kết quả', path: '/student/results', icon: Star },
    { label: 'Biểu mẫu', path: '/student/templates', icon: FileText, visibilitySetting: 'templatesVisible' },
    { label: 'Thư viện đề tài', path: '/student/library-topics', icon: BookOpen, visibilitySetting: 'libraryTopicsVisible' },
    { label: 'Nộp báo cáo bản cuối', path: '/student/final-submission', icon: FileCheck2 },
  ],
};

const sectionNames = { admin: 'BAN QUẢN LÝ KHOA', lecturer: 'KHÔNG GIAN GIẢNG VIÊN', student: 'CỔNG SINH VIÊN' };

const adminMenuGroups = [
  { id: 'overview', label: 'Tổng quan', icon: LayoutDashboard, items: menus.admin.slice(0, 1), standalone: true },
  { id: 'accounts', label: 'Tài khoản & quyền', icon: Users, items: [menus.admin[1], menus.admin[2], menus.admin[16]] },
  { id: 'education', label: 'Dữ liệu đào tạo', icon: School, items: [menus.admin[3], menus.admin[5], menus.admin[6]] },
  { id: 'projects', label: 'Quản lý đồ án', icon: BookOpen, items: [menus.admin[7], menus.admin[8], menus.admin[9], menus.admin[18], menus.admin[19], menus.admin[20]] },
  { id: 'defense', label: 'Tổ chức bảo vệ', icon: ShieldCheck, items: [menus.admin[4], menus.admin[10], menus.admin[11], menus.admin[12]] },
  { id: 'resources', label: 'Tài nguyên & lịch sử', icon: FileText, items: [menus.admin[13], menus.admin[14], menus.admin[15], menus.admin[17]] },
  { id: 'settings', label: 'Cài đặt chung', icon: Settings, items: [menus.admin[21]], standalone: true },
];
adminMenuGroups[4].items.push(menus.admin[22]);

export default function ApplicationLayout({ section }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [studentMenuOpen, setStudentMenuOpen] = useState(false);
  const [openAdminGroups, setOpenAdminGroups] = useState(() => new Set(['projects']));
  const [routeDelayDone, setRouteDelayDone] = useState(false);
  const [pendingRequests, setPendingRequests] = useState(() => getPendingHttpRequests());
  const [systemSettings, setSystemSettings] = useState({ libraryTopicsVisible: true, templatesVisible: true });
  const { user, logout } = useAuth();
  const toast = useToast();
  const location = useLocation();
  const previousPath = useRef(location.pathname);
  const navigate = useNavigate();
  const homePath = section === 'admin' ? '/admin' : section === 'lecturer' ? '/lecturer' : '/student';
  const active = useMemo(() => menus[section].find((item) => item.end ? location.pathname === item.path : location.pathname.startsWith(item.path)), [location.pathname, section]);
  const profileName = user?.fullName?.trim() || user?.userName?.trim() || 'Đang tải thông tin';
  const canViewMenuItem = (item) => {
    const allowed = !item.allowedRoles?.length || item.allowedRoles.some((role) => hasRole(user?.roles || [], role));
    return allowed && (!item.visibilitySetting || systemSettings[item.visibilitySetting] !== false);
  };
  const activeAdminGroup = useMemo(() => adminMenuGroups.find((group) => group.items.some((item) => item.end ? location.pathname === item.path : location.pathname.startsWith(item.path))), [location.pathname]);

  useEffect(() => {
    if (previousPath.current !== location.pathname) {
      toast.dismissAll();
      previousPath.current = location.pathname;
    }
  }, [location.pathname, toast]);

  useEffect(() => {
    setRouteDelayDone(false);
    const timer = window.setTimeout(() => setRouteDelayDone(true), 620);
    return () => window.clearTimeout(timer);
  }, [location.pathname]);

  useEffect(() => {
    const updatePending = (event) => setPendingRequests(event.detail);
    setPendingRequests(getPendingHttpRequests());
    window.addEventListener('app:http-pending', updatePending);
    return () => window.removeEventListener('app:http-pending', updatePending);
  }, []);

  useEffect(() => {
    let active = true;
    httpClient.get(API_ENDPOINTS.systemSettings.current)
      .then((response) => active && setSystemSettings((current) => ({ ...current, ...(response?.result ?? response) })))
      .catch(() => {});
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (section !== 'admin' || !activeAdminGroup || activeAdminGroup.standalone) return;
    setOpenAdminGroups((current) => {
      if (current.has(activeAdminGroup.id)) return current;
      const next = new Set(current);
      next.add(activeAdminGroup.id);
      return next;
    });
  }, [activeAdminGroup, section]);

  const toggleAdminGroup = (groupId) => {
    setOpenAdminGroups((current) => {
      const next = new Set(current);
      if (next.has(groupId)) next.delete(groupId);
      else next.add(groupId);
      return next;
    });
  };

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
        <nav className={`main-nav ${section === 'admin' ? 'grouped-nav' : ''}`} aria-label="Điều hướng chính">
          {section === 'admin' && !collapsed ? adminMenuGroups.map((group) => {
            const GroupIcon = group.icon;
            const expanded = group.standalone || openAdminGroups.has(group.id);
            const groupActive = activeAdminGroup?.id === group.id;
            if (group.standalone) {
              const item = group.items[0];
              if (!canViewMenuItem(item)) return null;
              return <NavLink key={item.path} to={item.path} end={item.end} onClick={() => setMobileOpen(false)} title={item.label}><GroupIcon size={19} /><span>{item.label}</span><ChevronRight className="nav-chevron" size={15} /></NavLink>;
            }
            return <div className={`nav-group ${expanded ? 'expanded' : ''} ${groupActive ? 'active-group' : ''}`} key={group.id}><button className="nav-group-trigger" type="button" onClick={() => toggleAdminGroup(group.id)} aria-expanded={expanded}><GroupIcon size={19} /><span>{group.label}</span><ChevronDown className="group-chevron" size={16} /></button><div className="nav-group-reveal" aria-hidden={!expanded}><div className="nav-group-items">{group.items.filter(canViewMenuItem).map(({ label, path, icon: Icon, end }) => <NavLink key={path} to={path} end={end} tabIndex={expanded ? 0 : -1} onClick={() => setMobileOpen(false)} title={label}><Icon size={17} /><span>{label}</span><ChevronRight className="nav-chevron" size={14} /></NavLink>)}</div></div></div>;
          }) : menus[section].filter(canViewMenuItem).map(({ label, path, icon: Icon, end }) => (
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
            <NotificationCenter section={section} />
            <div className="profile-wrap">
              <button className="profile-button" onClick={() => setProfileOpen((value) => !value)}>
                <span className="avatar">{section === 'student' ? initials(profileName) : section === 'lecturer' ? 'GV' : 'AD'}</span>
                <span className="profile-copy"><strong title={profileName}>{profileName}</strong><small>{sectionNames[section]}</small></span>
                <ChevronDown size={16} />
              </button>
              {profileOpen && <div className="profile-menu"><button onClick={() => { setProfileOpen(false); setPasswordModalOpen(true); }}><KeyRound size={17} /> Đổi mật khẩu</button><button className="logout" onClick={handleLogout}><LogOut size={17} /> Đăng xuất</button></div>}
            </div>
          </div>
        </header>
        {section === 'student' && <div className="student-body-menu"><div className="student-menu-wrap"><button className="student-menu-trigger" onClick={() => setStudentMenuOpen((value) => !value)} aria-expanded={studentMenuOpen}><Menu size={19} /><span>Chức năng</span><ChevronDown size={15} /></button>{studentMenuOpen && <nav className="student-header-menu" aria-label="Chức năng sinh viên">{menus.student.filter(canViewMenuItem).map(({ label, path, icon: Icon, end }) => <NavLink key={path} to={path} end={end} onClick={() => setStudentMenuOpen(false)}><Icon size={18} /><span>{label}</span><ChevronRight size={15} /></NavLink>)}</nav>}</div></div>}
        <main className="app-content"><Outlet /></main>
        {(!routeDelayDone || pendingRequests > 0) && <PageLoadingSkeleton message="Đang tải trang..." />}
      </div>
      {passwordModalOpen && <ChangePasswordModal onClose={() => setPasswordModalOpen(false)} />}
    </div>
  );
}

function ChangePasswordModal({ onClose }) {
  const toast = useToast();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    if (form.newPassword.length < 8 || form.newPassword.length > 72 || !/[A-Za-z]/.test(form.newPassword) || !/\d/.test(form.newPassword)) {
      setError('Mật khẩu mới phải dài 8-72 ký tự và có ít nhất một chữ cái, một chữ số.');
      return;
    }
    if (form.newPassword !== form.confirmPassword) {
      setError('Xác nhận mật khẩu mới không khớp.');
      return;
    }
    if (form.currentPassword === form.newPassword) {
      setError('Mật khẩu mới phải khác mật khẩu hiện tại.');
      return;
    }
    setSaving(true);
    try {
      await httpClient.patch(API_ENDPOINTS.auth.changePassword, form);
      toast.success('Đổi mật khẩu thành công.');
      onClose();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không thể đổi mật khẩu. Vui lòng thử lại.'));
    } finally {
      setSaving(false);
    }
  };

  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && !saving && onClose()}><section className="change-password-modal" role="dialog" aria-modal="true" aria-labelledby="change-password-title"><header><div className="change-password-icon"><KeyRound size={23} /></div><div><span>BẢO MẬT TÀI KHOẢN</span><h3 id="change-password-title">Đổi mật khẩu</h3></div><button type="button" aria-label="Đóng" disabled={saving} onClick={onClose}><X size={19} /></button></header><form onSubmit={submit}><PasswordField label="Mật khẩu hiện tại" value={form.currentPassword} autoComplete="current-password" disabled={saving} onChange={(value) => setForm((current) => ({ ...current, currentPassword: value }))} /><PasswordField label="Mật khẩu mới" value={form.newPassword} autoComplete="new-password" disabled={saving} onChange={(value) => setForm((current) => ({ ...current, newPassword: value }))} /><small className="password-policy">Từ 8 đến 72 ký tự, gồm ít nhất một chữ cái và một chữ số.</small><PasswordField label="Xác nhận mật khẩu mới" value={form.confirmPassword} autoComplete="new-password" disabled={saving} onChange={(value) => setForm((current) => ({ ...current, confirmPassword: value }))} />{error && <div className="change-password-error">{error}</div>}<footer><button type="button" className="secondary-button" disabled={saving} onClick={onClose}>Hủy</button><button type="submit" className="primary-button" disabled={saving || !form.currentPassword || !form.newPassword || !form.confirmPassword}>{saving ? 'Đang đổi...' : 'Đổi mật khẩu'}</button></footer></form></section></div>;
}

function PasswordField({ label, value, onChange, ...inputProps }) {
  const [visible, setVisible] = useState(false);
  return <label className="change-password-field"><span>{label}<b>*</b></span><div><input {...inputProps} required type={visible ? 'text' : 'password'} value={value} onChange={(event) => onChange(event.target.value)} /><button type="button" aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} onClick={() => setVisible((current) => !current)}>{visible ? <EyeOff size={17} /> : <Eye size={17} />}</button></div></label>;
}

function initials(value) {
  const parts = String(value).trim().split(/\s+/).filter(Boolean);
  if (!parts.length || value === 'Đang tải thông tin') return 'SV';
  return parts.slice(-2).map((part) => part[0]).join('').toUpperCase();
}
