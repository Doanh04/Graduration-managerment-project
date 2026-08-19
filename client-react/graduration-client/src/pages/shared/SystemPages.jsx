import React from 'react';
import { ArrowLeft, LockKeyhole, SearchX } from 'lucide-react';
import { Link } from 'react-router-dom';
import logo from '../../img/sv_logo_dashboard.png';

function SystemPage({ denied = false }) {
  const Icon = denied ? LockKeyhole : SearchX;
  return <main className="system-page"><img src={logo} alt="Đại học Công nghiệp Việt – Hung" /><div className="system-icon"><Icon size={34} /></div><span>{denied ? '403' : '404'}</span><h1>{denied ? 'Bạn không có quyền truy cập' : 'Không tìm thấy trang'}</h1><p>{denied ? 'Tài khoản hiện tại không được cấp quyền sử dụng chức năng này.' : 'Đường dẫn bạn đang truy cập không tồn tại hoặc đã được thay đổi.'}</p><Link to="/"><ArrowLeft size={17} /> Quay về trang chính</Link></main>;
}

export const AccessDeniedPage = () => <SystemPage denied />;
export const NotFoundPage = () => <SystemPage />;
