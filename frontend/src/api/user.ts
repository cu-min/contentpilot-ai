import request, { ApiResult } from './request';
import type { PageResult, UserInfo, UserRole } from '../types';

export interface UserListParams {
  page: number;
  size: number;
  keyword?: string;
}

export interface CreateUserPayload {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
  role: UserRole;
}

export interface UpdateUserPayload {
  nickname?: string;
  email?: string;
  role?: UserRole;
  status?: number;
}

export function getUsers(params: UserListParams) {
  return request.get<unknown, ApiResult<PageResult<UserInfo>>>('/users', { params });
}

export function createUser(payload: CreateUserPayload) {
  return request.post<unknown, ApiResult<UserInfo>>('/users', payload);
}

export function updateUser(id: number, payload: UpdateUserPayload) {
  return request.put<unknown, ApiResult<UserInfo>>(`/users/${id}`, payload);
}

export function updateUserStatus(id: number, status: number) {
  return request.put<unknown, ApiResult<null>>(`/users/${id}/status`, { status });
}
