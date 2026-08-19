import React, { useState } from 'react';
import {
  User,
  Lock,
  Eye,
  EyeOff,
  AlertCircle,
  CheckCircle2,
  BookOpen,
  Award,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  GraduationCap,
  FileCheck,
} from 'lucide-react';

import logoImg from '../img/sv_logo_dashboard.png';
import bgNghiemThu from '../img/nghiem-thu-800x450.jpg';
import bannerImg from '../img/thesis_defense_banner.jpg';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import '../style/LoginLayout.scss';

const ACCOUNT_INACTIVE_ERROR_CODE = 1073;

const getLoginErrorMessage = (errorResponse) => {
  if (errorResponse?.code === ACCOUNT_INACTIVE_ERROR_CODE) {
    return 'Tài khoản đã dừng hoạt động';
  }

  return errorResponse?.message || 'Tên đăng nhập hoặc mật khẩu không chính xác.';
};

export default function LoginLayout() {
  const { login } = useAuth();
  const toast = useToast();

  const [userName, setUserName] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!userName.trim()) {
      setError('Vui lòng nhập Tên đăng nhập (Mã Sinh viên / Mã Giảng viên)');
      toast.error('Vui lòng nhập Tên đăng nhập (Mã Sinh viên / Mã Giảng viên)', { title: 'Thiếu thông tin đăng nhập' });
      return;
    }
    if (!password) {
      setError('Vui lòng nhập Mật khẩu');
      toast.error('Vui lòng nhập Mật khẩu', { title: 'Thiếu thông tin đăng nhập' });
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await login({
        identifier: userName.trim(),
        password,
      });

      if (response && (response.code === 1000 || response.result)) {
        setSuccess('Đăng nhập thành công!');
        toast.success('Đăng nhập thành công!');
      } else {
        const message = getLoginErrorMessage(response);
        setError(message);
        toast.error(message, { title: 'Đăng nhập thất bại' });
      }
    } catch (err) {
      console.error('Lỗi khi đăng nhập:', err);
      setError(getLoginErrorMessage(err));
      toast.error(getLoginErrorMessage(err), { title: 'Đăng nhập thất bại' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-page-container">
      <div className="login-left-panel">
        <div className="login-header-brand">
          <img
            src={logoImg}
            alt="Logo ĐH Công Nghiệp Việt - Hưng"
            className="h-14 w-auto object-contain max-h-16"
          />
        </div>

        <div className="login-form-wrapper">
          <div className="login-system-tag">
            <Sparkles size={16} />
            <span>Hệ Thống Quản Lý Đồ Án Tốt Nghiệp</span>
          </div>

          <h1 className="login-title">Cổng Đăng Nhập</h1>
          <p className="login-subtitle">
            Dành cho Sinh viên, Giảng viên & Hội đồng Trường ĐH Công Nghiệp Việt - Hung
          </p>

          {error && (
            <div className="login-alert-error">
              <AlertCircle size={20} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="login-alert-success">
              <CheckCircle2 size={20} className="shrink-0" />
              <span>{success}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate>
            <div className="login-input-group">
              <label htmlFor="userName" className="login-input-label">
                Tên đăng nhập / Mã SV / Mã GV
              </label>
              <div className="login-input-container">
                <input
                  id="userName"
                  type="text"
                  className="login-input"
                  placeholder="Nhập mã sinh viên hoặc mã giảng viên..."
                  value={userName}
                  onChange={(e) => setUserName(e.target.value)}
                  disabled={isSubmitting}
                  autoComplete="username"
                />
                <User className="login-input-icon" size={18} />
              </div>
            </div>

            <div className="login-input-group">
              <label htmlFor="password" className="login-input-label">
                Mật khẩu
              </label>
              <div className="login-input-container">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  className="login-input"
                  placeholder="Nhập mật khẩu truy cập..."
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={isSubmitting}
                  autoComplete="current-password"
                />
                <Lock className="login-input-icon" size={18} />
                <button
                  type="button"
                  className="login-password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              className="login-submit-button"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <div className="spinner"></div>
                  <span>Đang xác thực...</span>
                </>
              ) : (
                <>
                  <span>ĐĂNG NHẬP HỆ THỐNG</span>
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>
        </div>

        <div className="login-footer">
          <p>© {new Date().getFullYear()} Trường Đại Học Công Nghiệp Việt - Hưng</p>
          <p className="mt-1 text-slate-400">Phát triển phục vụ công tác Quản lý Đồ án & Khóa luận Tốt nghiệp</p>
        </div>
      </div>

      <div className="login-right-panel">
        <div
          className="login-right-bg-image"
          style={{ backgroundImage: `url(${bgNghiemThu})` }}
        ></div>
        <div className="login-right-overlay"></div>

        <div className="login-illustration-card">
          <div className="login-card-header-badge">
            <div className="login-badge-pill">
              <ShieldCheck size={16} />
              <span>Hệ Thống Số Hóa Đào Tạo</span>
            </div>
            <span className="login-status-pill">
              <span className="pulse-dot"></span> Đợt Bảo Vệ Đang Mở
            </span>
          </div>

          <div className="login-banner-image-container">
            <img
              src={bannerImg}
              alt="Sinh viên và Giảng viên bảo vệ đồ án tốt nghiệp"
              className="login-banner-img"
            />
            <div className="image-caption-tag">
              <GraduationCap size={15} />
              <span>Bảo Vệ & Phản Biện Khóa Luận Tốt Nghiệp</span>
            </div>
          </div>

          <div className="login-visual-feature-grid">
            <div className="feature-card">
              <div className="icon-box">
                <BookOpen size={18} />
              </div>
              <span className="feature-title">Đăng Ký Đề Tài</span>
              <span className="feature-sub">Duyệt trực tuyến</span>
            </div>

            <div className="feature-card">
              <div className="icon-box">
                <FileCheck size={18} />
              </div>
              <span className="feature-title">Nhật Ký Tiến Độ</span>
              <span className="feature-sub">Báo cáo tuần</span>
            </div>

            <div className="feature-card">
              <div className="icon-box">
                <Award size={18} />
              </div>
              <span className="feature-title">Hội Đồng Bảo Vệ</span>
              <span className="feature-sub">Chấm điểm & Tổng kết</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
