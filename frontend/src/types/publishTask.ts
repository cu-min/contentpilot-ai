import type { PlatformContentPlatform } from './platformContent';
import type { PublishMode } from './platformAccount';

export type PublishTaskStatus =
  | 'DRAFT'
  | 'PENDING'
  | 'CANCELLED'
  | 'RUNNING'
  | 'SUCCESS'
  | 'FAILED';

export type PublishType = 'IMMEDIATE' | 'SCHEDULED';

export type PublishArticleStatus =
  | 'PUBLISHED'
  | 'FAILED'
  | 'CANCELLED';

export interface PublishTask {
  id: number;
  articleId: number;
  platformContentId: number;
  platformContentTitle?: string;
  platform: PlatformContentPlatform;
  accountId: number;
  accountName?: string;
  title: string;
  status: PublishTaskStatus;
  publishType: PublishType;
  scheduleTime?: string;
  publishMode: PublishMode;
  publishUrl?: string;
  externalDraftId?: string;
  externalPublishId?: string;
  externalArticleId?: string;
  draftUrl?: string;
  articleStatus?: PublishArticleStatus;
  errorMessage?: string;
  createdBy?: number;
  updatedBy?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PublishTaskQuery {
  page: number;
  size: number;
  platform?: PlatformContentPlatform;
  status?: PublishTaskStatus;
  publishType?: PublishType;
}

export interface PublishTaskPayload {
  platformContentId: number;
  accountId: number;
  title?: string;
  publishType?: PublishType;
  scheduleTime?: string;
}

export interface PublishTaskSubmitPayload {
  publishType: PublishType;
  scheduleTime?: string;
}

export const publishTypeOptions = [
  { label: '立即发布', value: 'IMMEDIATE' },
  { label: '定时发布', value: 'SCHEDULED' },
] as const;

export const publishTaskStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待发布', value: 'PENDING' },
  { label: '执行中', value: 'RUNNING' },
  { label: '执行成功', value: 'SUCCESS' },
  { label: '执行失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELLED' },
] as const;

export function getPublishTypeLabel(type?: string) {
  return publishTypeOptions.find((option) => option.value === type)?.label || type || '-';
}

export function getPublishTaskStatusLabel(status?: string) {
  return publishTaskStatusOptions.find((option) => option.value === status)?.label || status || '-';
}
