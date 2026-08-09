import httpClient from '../../config/HttpClient.jsx';

/**
 * Service xử lý các API Xác thực (Authentication) tương ứng với AuthenticationCookieControler ở Backend.
 * Backend hiện tại đã hỗ trợ đăng nhập linh hoạt bằng Identifier (Tên đăng nhập / Mã Sinh viên / Mã Giảng viên).
 */
export const AuthenticationService = {
  /**
   * Đăng nhập người dùng bằng HttpOnly Cookie
   * @param {Object} payload - { identifier, password }
   * @returns {Promise<Object>} - Trả về dữ liệu ApiResponse { code, message, result } từ Backend
   */
  login: ({ identifier, password }) => {
    const loginIdentifier = (identifier || '').trim();
    return httpClient.post('/auth/cookie/login', {
      identifier: loginIdentifier,
      password,
    });
  },

  /**
   * Kiểm tra tính hợp lệ của Token hiện tại trong Cookie HttpOnly
   * @returns {Promise<Object>} - Trả về ApiResponse { code, result: { valid: boolean } }
   */
  introspect: () => {
    return httpClient.post('/auth/cookie/introspect');
  },

  /**
   * Làm mới Access Token bằng Cookie HttpOnly
   * @returns {Promise<Object>} - Trả về ApiResponse chứa dữ liệu token mới
   */
  refresh: () => {
    return httpClient.post('/auth/cookie/refresh');
  },

  /**
   * Đăng xuất người dùng và thu hồi/xóa Cookie HttpOnly
   * @returns {Promise<Object>} - Trả về ApiResponse đăng xuất từ Backend
   */
  logout: () => {
    return httpClient.post('/auth/cookie/logout');
  },
};

export default AuthenticationService;
