import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/graduration';


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
