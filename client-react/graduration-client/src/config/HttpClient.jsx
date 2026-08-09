import axios from 'axios';

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/graduration';

/**
 * HttpClient với Axios được cấu hình tập trung cho toàn ứng dụng.
 * - withCredentials: true: Bắt buộc gửi & nhận HttpOnly Cookie ('access_token')
 * - Tự động gọi API /auth/cookie/refresh khi Token hết hạn (401 / Code 1019) và thử lại request ban đầu
 */
const httpClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, data = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(data);
    }
  });
  failedQueue = [];
};

// Request Interceptor
httpClient.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
);

// Response Interceptor với cơ chế tự động Refresh Token
httpClient.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const originalRequest = error.config;

    // Không kích hoạt refresh tự động cho các API đăng nhập/đăng xuất/refresh bản thân nó
    const isAuthEndpoint =
      originalRequest?.url?.includes('/auth/cookie/login') ||
      originalRequest?.url?.includes('/auth/cookie/refresh') ||
      originalRequest?.url?.includes('/auth/cookie/logout') ||
      originalRequest?.url?.includes('/auth/cookie/introspect');

    // Nếu gặp lỗi 401 (UNAUTHORIZED) hoặc code 1019 (UNAUTHENTICATED) và chưa từng retry
    if (
      error.response &&
      originalRequest &&
      (error.response.status === 401 || error.response.data?.code === 1019) &&
      !originalRequest._retry &&
      !isAuthEndpoint
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => httpClient(originalRequest))
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Tự động gọi API Refresh Token ở backend
        const refreshResponse = await axios.post(
          `${API_BASE_URL}/auth/cookie/refresh`,
          {},
          { withCredentials: true }
        );

        if (
          refreshResponse.data &&
          (refreshResponse.data.code === 1000 || refreshResponse.data.result)
        ) {
          processQueue(null, refreshResponse.data);
          // Thử lại request ban đầu sau khi có token mới trong cookie
          return httpClient(originalRequest);
        } else {
          processQueue(refreshResponse.data, null);
          return Promise.reject(refreshResponse.data);
        }
      } catch (refreshError) {
        const errorData = refreshError.response?.data || {
          code: 1019,
          message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        };
        processQueue(errorData, null);
        return Promise.reject(errorData);
      } finally {
        isRefreshing = false;
      }
    }

    // Trả về dữ liệu lỗi chuẩn ApiResponse từ backend
    if (error.response && error.response.data) {
      return Promise.reject(error.response.data);
    }

    return Promise.reject({
      code: 9999,
      message: error.message || 'Không thể kết nối tới máy chủ.',
    });
  }
);

export default httpClient;
