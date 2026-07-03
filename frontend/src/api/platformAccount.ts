import request, { ApiResult } from './request';
import type {
  PlatformAccount,
  PlatformAccountPayload,
  PlatformAccountQuery,
} from '../types/platformAccount';

export function getPlatformAccounts(params?: PlatformAccountQuery) {
  return request.get<unknown, ApiResult<PlatformAccount[]>>('/platform-accounts', { params });
}

export function getPlatformAccountDetail(id: number) {
  return request.get<unknown, ApiResult<PlatformAccount>>(`/platform-accounts/${id}`);
}

export function createPlatformAccount(data: PlatformAccountPayload) {
  return request.post<unknown, ApiResult<PlatformAccount>>('/platform-accounts', data);
}

export function updatePlatformAccount(id: number, data: PlatformAccountPayload) {
  return request.put<unknown, ApiResult<PlatformAccount>>(`/platform-accounts/${id}`, data);
}

export function updatePlatformAccountStatus(id: number, enabled: number) {
  return request.put<unknown, ApiResult<null>>(`/platform-accounts/${id}/status`, { enabled });
}
