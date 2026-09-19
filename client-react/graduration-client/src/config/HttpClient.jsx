import axios from 'axios';

// Có thể ghi đè khi build Docker; mặc định giữ nguyên địa chỉ dùng cho môi trường local.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/graduration';


const httpClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

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
    pendingRequests += 1;
    config.__trackedRequest = true;
    notifyPendingRequests();
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
  (error) => {
    if (error.config?.__trackedRequest) finishRequest();
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
