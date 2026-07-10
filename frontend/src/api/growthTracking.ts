import request, { ApiResult } from './request';
import type {
  GrowthTrackingTarget,
  GrowthTrackingTargetPayload,
  GrowthTrackingTargetQuery,
} from '../types/growthTracking';

export function getGrowthTrackingTargets(params?: GrowthTrackingTargetQuery) {
  return request.get<unknown, ApiResult<GrowthTrackingTarget[]>>('/growth/tracking-targets', { params });
}

export function createGrowthTrackingTarget(data: GrowthTrackingTargetPayload) {
  return request.post<unknown, ApiResult<GrowthTrackingTarget>>('/growth/tracking-targets', data);
}

export function updateGrowthTrackingTarget(id: number, data: GrowthTrackingTargetPayload) {
  return request.put<unknown, ApiResult<GrowthTrackingTarget>>(`/growth/tracking-targets/${id}`, data);
}

export function updateGrowthTrackingTargetStatus(id: number, enabled: number) {
  return request.put<unknown, ApiResult<null>>(`/growth/tracking-targets/${id}/status`, { enabled });
}

export function deleteGrowthTrackingTarget(id: number) {
  return request.delete<unknown, ApiResult<null>>(`/growth/tracking-targets/${id}`);
}

export function checkGrowthTrackingTarget(id: number) {
  return request.post<unknown, ApiResult<GrowthTrackingTarget>>(`/growth/tracking-targets/${id}/check`);
}
