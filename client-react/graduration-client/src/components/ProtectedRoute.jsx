import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import PageLoadingSkeleton from './feedback/PageLoadingSkeleton.jsx';

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
    return <PageLoadingSkeleton fullScreen message="Đang xác thực phiên làm việc..." />;
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
