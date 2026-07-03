import axios from 'axios';
import { TOKEN_KEY, authStore } from '../stores';

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

const request = axios.create({
  baseURL: '/api',
  timeout: 120000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>;
    if (result.code !== 200) {
      return Promise.reject(new Error(result.message || '请求失败'));
    }
    return response.data;
  },
  (error) => {
    if (error.response?.status === 401) {
      authStore.clearAuth();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

export default request;
