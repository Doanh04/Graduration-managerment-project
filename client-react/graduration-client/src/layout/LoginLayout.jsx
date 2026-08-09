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
import '../style/LoginLayout.scss';

export default function LoginLayout() {
  const { login } = useAuth();

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
      return;
    }
    if (!password) {
      setError('Vui lòng nhập Mật khẩu');
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
      } else {
        setError(response?.message || 'Đăng nhập không thành công.');
      }
    } catch (err) {
      console.error('Lỗi khi đăng nhập:', err);
      const backendMessage = err?.message || 'Tên đăng nhập hoặc mật khẩu không chính xác.';
      setError(backendMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-page-container">
      {/* Cột Trái - Form Đăng Nhập (Giữ Nền Trắng Sạch Sẽ & Tách Biệt) */}
      <div className="login-left-panel">
        {/* Logo Trường từ sv_logo_dashboard.png */}
        <div className="login-header-brand">
          <img
            src={logoImg}
            alt="Logo ĐH Công Nghiệp Việt - Hưng"
            className="h-14 w-auto object-contain max-h-16"
          />
        </div>

        {/* Khối Ô Form Đăng Nhập */}
        <div className="login-form-wrapper">
          <div className="login-system-tag">
            <Sparkles size={16} />
            <span>Hệ Thống Quản Lý Đồ Án Tốt Nghiệp</span>
          </div>

          <h1 className="login-title">Cổng Đăng Nhập</h1>
          <p className="login-subtitle">
            Dành cho Sinh viên, Giảng viên & Hội đồng Trường ĐH Công Nghiệp Việt - Hưng
          </p>

          {/* Thông báo lỗi */}
          {error && (
            <div className="login-alert-error">
              <AlertCircle size={20} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Thông báo thành công */}
          {success && (
            <div className="login-alert-success">
              <CheckCircle2 size={20} className="shrink-0" />
              <span>{success}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate>
            {/* Input Username / Mã SV / Mã GV */}
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

            {/* Input Password */}
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

            {/* Nút Đăng nhập */}
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

        {/* Footer Bản Quyền */}
        <div className="login-footer">
          <p>© {new Date().getFullYear()} Trường Đại Học Công Nghiệp Việt - Hưng</p>
          <p className="mt-1 text-slate-400">Phát triển phục vụ công tác Quản lý Đồ án & Khóa luận Tốt nghiệp</p>
        </div>
      </div>

      {/* Cột Phải - Sử dụng nghiem-thu-800x450.jpg làm Background & Làm Mờ */}
      <div className="login-right-panel">
        {/* Layer Background Ảnh nghiem-thu-800x450.jpg được Làm Mờ (Blur Effect) */}
        <div
          className="login-right-bg-image"
          style={{ backgroundImage: `url(${bgNghiemThu})` }}
        ></div>
        <div className="login-right-overlay"></div>

        {/* Card Nội Dung Trong Suốt Đặt Trên Background nghiem-thu */}
        <div className="login-illustration-card">
          {/* Badge Trạng thái */}
          <div className="login-card-header-badge">
            <div className="login-badge-pill">
              <ShieldCheck size={16} />
              <span>Hệ Thống Số Hóa Đào Tạo</span>
            </div>
            <span className="login-status-pill">
              <span className="pulse-dot"></span> Đợt Bảo Vệ Đang Mở
            </span>
          </div>

          {/* Banner Trực Quan Sinh Viên & Giảng Viên */}
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

          {/* Grid Tính Năng Trực Quan */}
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
