import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/graduration';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

export const AuthenticationService = {
  /**
   * Đăng nhập người dùng bằng HttpOnly Cookie
   * @param {Object} credentials 
   * @returns {Promise<Object>} - Trả về dữ liệu ApiResponse từ Backend { code, message, result }
   */
  login: async ({ identifier, password }) => {
    try {
      const response = await api.post('/auth/cookie/login', { identifier, password });
      return response.data;
    } catch (error) {
      if (error.response && error.response.data) {
        throw error.response.data;
      }
      throw {
        code: 9999,
        message: error.message || 'Đăng nhập thất bại. Không thể kết nối tới server.',
      };
    }
  },

  /**
   * Kiểm tra tính hợp lệ của Token hiện tại trong Cookie HttpOnly
   * @returns {Promise<Object>} - Trả về ApiResponse { code, result: { valid: boolean } }
   */
  introspect: async () => {
    try {
      const response = await api.post('/auth/cookie/introspect');
      return response.data;
    } catch (error) {
      if (error.response && error.response.data) {
        throw error.response.data;
      }
      throw {
        code: 9999,
        message: error.message || 'Kiểm tra token thất bại.',
      };
    }
  },

  /**
   * Làm mới Access Token bằng Cookie HttpOnly
   * @returns {Promise<Object>} - Trả về ApiResponse chứa dữ liệu token mới
   */
  refresh: async () => {
    try {
      const response = await api.post('/auth/cookie/refresh');
      return response.data;
    } catch (error) {
      if (error.response && error.response.data) {
        throw error.response.data;
      }
      throw {
        code: 9999,
        message: error.message || 'Refresh token thất bại.',
      };
    }
  },

  profile: async () => {
    try {
      const response = await api.get('/auth/cookie/me');
      return response.data;
    } catch (error) {
      if (error.response?.data) throw error.response.data;
      throw { code: 9999, message: error.message || 'Không thể tải thông tin tài khoản.' };
    }
  },

  /**
   * Đăng xuất người dùng và thu hồi/xóa Cookie HttpOnly
   * @returns {Promise<Object>} - Trả về ApiResponse đăng xuất từ Backend
   */
  logout: async () => {
    try {
      const response = await api.post('/auth/cookie/logout');
      return response.data;
    } catch (error) {
      if (error.response && error.response.data) {
        throw error.response.data;
      }
      throw {
        code: 9999,
        message: error.message || 'Đăng xuất thất bại.',
      };
    }
  },
};

export default AuthenticationService;
