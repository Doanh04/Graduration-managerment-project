import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

/**
 * Route Guard bảo vệ các route nội bộ:
 * - Kiểm tra cookie & phiên làm việc
 * - Ngăn người dùng chưa đăng nhập truy cập
 * - Phân quyền vai trò (Role-based access: Student vs Lecturer vs Admin)
 */
export default function ProtectedRoute({ children, allowedAccountTypes = [], allowedRoles = [] }) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const location = useLocation();

  // Đang kiểm tra phiên làm việc (khi F5 / tải lại trang)
  if (isLoading) {
    return (
      <div className="flex min-h-screen w-full items-center justify-center bg-slate-50 text-slate-700 font-sans">
        <div className="flex flex-col items-center gap-4 p-8 rounded-2xl bg-white shadow-xl border border-slate-100">
          <div className="w-10 h-10 border-4 border-cyan-200 border-t-cyan-600 rounded-full animate-spin"></div>
          <p className="text-sm font-semibold text-slate-600">Đang xác thực phiên làm việc...</p>
        </div>
      </div>
    );
  }

  // Chưa đăng nhập -> Chuyển hướng tới trang Đăng nhập
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Đã đăng nhập nhưng kiểm tra phân quyền accountType (nếu có yêu cầu)
  if (!user || !user.accountType) {
    return <Navigate to="/login" replace />;
  }

  if (allowedAccountTypes.length > 0) {
    const hasPermission = allowedAccountTypes.includes(user.accountType);
    if (!hasPermission) {
      // Điều hướng về trang chủ theo vai trò thực tế của user
      if (user.accountType === 'STUDENT') {
        return <Navigate to="/student" replace />;
      } else if (user.accountType === 'LECTURER') {
        return <Navigate to="/lecturer" replace />;
      } else {
        return <Navigate to="/dashboard" replace />;
      }
    }
  }

  if (allowedRoles.length > 0) {
    const userRoles = user.roles || [];
    if (!allowedRoles.some((role) => userRoles.includes(role))) {
      return <Navigate to="/access-denied" replace />;
    }
  }

  return children;
}
