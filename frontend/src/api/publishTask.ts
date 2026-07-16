import request, { ApiResult } from './request';
import type { PageResult } from '../types';
import type {
  PublishTask,
  PublishTaskPayload,
  PublishTaskQuery,
  PublishTaskSubmitPayload,
} from '../types/publishTask';

export function getPublishTasks(params: PublishTaskQuery) {
  return request.get<unknown, ApiResult<PageResult<PublishTask>>>('/publish/tasks', { params });
}

export function getPublishTaskDetail(id: number) {
  return request.get<unknown, ApiResult<PublishTask>>(`/publish/tasks/${id}`);
}

export function createPublishTask(data: PublishTaskPayload) {
  return request.post<unknown, ApiResult<PublishTask>>('/publish/tasks', data);
}

export function updatePublishTask(id: number, data: PublishTaskPayload) {
  return request.put<unknown, ApiResult<PublishTask>>(`/publish/tasks/${id}`, data);
}

export function submitPublishTask(id: number, data: PublishTaskSubmitPayload) {
  return request.put<unknown, ApiResult<null>>(`/publish/tasks/${id}/submit`, data);
}

export function cancelPublishTask(id: number) {
  return request.put<unknown, ApiResult<null>>(`/publish/tasks/${id}/cancel`);
}

export function preparePublishTask(id: number) {
  return request.post<unknown, ApiResult<PublishTask>>(`/publish/tasks/${id}/prepare`);
}
