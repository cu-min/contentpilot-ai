import request, { ApiResult } from './request';
import type { LoginResponse, UserInfo } from '../types';

export interface LoginPayload {
  username: string;
  password: string;
}

export function login(payload: LoginPayload) {
  return request.post<unknown, ApiResult<LoginResponse>>('/auth/login', payload);
}

export function getCurrentUser() {
  return request.get<unknown, ApiResult<UserInfo>>('/auth/me');
}
