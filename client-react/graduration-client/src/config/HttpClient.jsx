import axios from 'axios';

// Docker/Nginx có thể ghi đè bằng VITE_API_BASE_URL. Khi chạy Vite trực tiếp,
// dùng đúng hostname đang mở trang để cả localhost và máy khác trong LAN đều
// gọi về backend trên cùng máy, tránh cookie bị mất do trộn localhost với IP.
const browserApiUrl = typeof window === 'undefined'
  ? 'http://localhost:8080/graduration'
  : `${window.location.protocol}//${window.location.hostname}:8080/graduration`;
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || browserApiUrl;


const httpClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

const AUTH_SESSION_MARKER = 'auth_session_present';
let refreshRequest = null;

const refreshAccessToken = () => {
  if (!refreshRequest) {
    // Dùng axios gốc để request refresh không đi qua interceptor này và không
    // tạo vòng lặp 401 -> refresh -> 401.
    refreshRequest = axios.post(
      `${API_BASE_URL}/auth/cookie/refresh`,
      null,
      { withCredentials: true },
    ).finally(() => {
      refreshRequest = null;
    });
  }
  return refreshRequest;
};

const expireClientSession = () => {
  if (typeof window === 'undefined') return;
  window.localStorage.removeItem(AUTH_SESSION_MARKER);
  window.dispatchEvent(new CustomEvent('app:auth-session-expired'));
};

let pendingRequests = 0;
const notifyPendingRequests = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('app:http-pending', { detail: pendingRequests }));
  }
};
const finishRequest = () => {
  pendingRequests = Math.max(0, pendingRequests - 1);
  notifyPendingRequests();
};
export const getPendingHttpRequests = () => pendingRequests;

httpClient.interceptors.request.use(
  (config) => {
    // Để Axios tự sinh Content-Type kèm boundary cho FormData. Nếu giữ
    // header application/json mặc định, Spring sẽ không nhận được multipart.
    const isFormData = typeof FormData !== 'undefined'
      && (config.data instanceof FormData || Object.prototype.toString.call(config.data) === '[object FormData]');
    if (isFormData) {
      if (config.headers?.delete) {
        // AxiosHeaders is case-insensitive, but remove both spellings for
        // compatibility with plain-object adapters and older Axios builds.
        config.headers.delete('Content-Type');
        config.headers.delete('content-type');
      } else if (config.headers) {
        delete config.headers['Content-Type'];
        delete config.headers['content-type'];
        if (config.headers.common) {
          delete config.headers.common['Content-Type'];
          delete config.headers.common['content-type'];
        }
      }
    }
    if (!config.skipGlobalLoading) {
      pendingRequests += 1;
      config.__trackedRequest = true;
      notifyPendingRequests();
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

httpClient.interceptors.response.use(
  (response) => {
    if (response.config?.__trackedRequest) finishRequest();
    return response.data;
  },
  async (error) => {
    if (error.config?.__trackedRequest) finishRequest();
    const originalRequest = error.config;
    const status = error.response?.status;
    const isAuthenticationRequest = typeof originalRequest?.url === 'string'
      && originalRequest.url.includes('/auth/cookie/');

    if (status === 401 && originalRequest && !originalRequest.__authRetried && !isAuthenticationRequest) {
      originalRequest.__authRetried = true;
      try {
        await refreshAccessToken();
        return httpClient.request(originalRequest);
      } catch (refreshError) {
        expireClientSession();
        const refreshData = refreshError.response?.data;
        return Promise.reject(refreshData || {
          code: 1019,
          message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        });
      }
    }
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
