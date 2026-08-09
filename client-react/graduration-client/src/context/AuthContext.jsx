import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import AuthenticationService from '../Service/Auth/AuthenticationService.jsx';

const AuthContext = createContext(null);
const AUTH_SESSION_MARKER = 'auth_session_present';

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const initialRestoreStarted = useRef(false);
  /**
   * Khôi phục phiên làm việc khi F5 / Tải lại trang:
   * Tự động kiểm tra Cookie HttpOnly còn hiệu lực hay không bằng cách gọi refresh/introspect
   * và khôi phục thông tin accountType, roles cho Client.
   */
  const restoreSession = useCallback(async () => {
    setIsLoading(true);
    try {
      // Gọi refresh để kiểm tra cookie và lấy thông tin accountType, roles mới nhất
      const res = await AuthenticationService.refresh();
      if (res && res.result && res.result.authenticated) {
        setUser(res.result);
        setIsAuthenticated(true);
      } else {
        localStorage.removeItem(AUTH_SESSION_MARKER);
        setUser(null);
        setIsAuthenticated(false);
      }
    } catch (refreshErr) {
      console.debug('Khôi phục phiên thất bại:', refreshErr);
      localStorage.removeItem(AUTH_SESSION_MARKER);
      setUser(null);
      setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (initialRestoreStarted.current) {
      return;
    }
    initialRestoreStarted.current = true;
    if (localStorage.getItem(AUTH_SESSION_MARKER) === 'true') {
      restoreSession();
    } else {
      setIsLoading(false);
    }
  }, [restoreSession]);

  /**
   * Đăng nhập người dùng
   */
  const login = async ({ identifier, password }) => {
    const response = await AuthenticationService.login({
      identifier,
      password,
    });

    if (response && (response.code === 1000 || response.result)) {
      const userData = response.result;
      localStorage.setItem(AUTH_SESSION_MARKER, 'true');
      setUser(userData);
      setIsAuthenticated(true);
    }
    return response;
  };

  /**
   * Đăng xuất người dùng
   */
  const logout = async () => {
    try {
      await AuthenticationService.logout();
    } catch (err) {
      console.warn('Lỗi khi đăng xuất:', err);
    } finally {
      localStorage.removeItem(AUTH_SESSION_MARKER);
      setUser(null);
      setIsAuthenticated(false);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        isLoading,
        login,
        logout,
        restoreSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth phải được sử dụng bên trong AuthProvider');
  }
  return context;
};

export default AuthContext;
