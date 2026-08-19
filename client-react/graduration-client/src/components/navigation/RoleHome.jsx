import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import LoginLayout from '../../layout/LoginLayout.jsx';

const getHomePath = (user) => {
  const roles = user?.roles || [];
  if (roles.includes('ADMIN') || roles.includes('FACULTY') || user?.accountType === 'USER') {
    return '/admin';
  }
  if (user?.accountType === 'LECTURER') {
    return '/lecturer';
  }
  return '/student';
};

export function RoleHome() {
  const { isAuthenticated, isLoading, user } = useAuth();
  if (isLoading) return <div className="app-boot">Đang khởi tạo hệ thống...</div>;
  return <Navigate to={isAuthenticated ? getHomePath(user) : '/login'} replace />;
}

export function PublicLoginRoute() {
  const { isAuthenticated, isLoading, user } = useAuth();
  if (isLoading) return <div className="app-boot">Đang kiểm tra phiên đăng nhập...</div>;
  return isAuthenticated ? <Navigate to={getHomePath(user)} replace /> : <LoginLayout />;
}
