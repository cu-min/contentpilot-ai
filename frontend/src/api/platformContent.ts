import request, { ApiResult } from './request';
import type {
  ArticlePlatformContent,
  PlatformContentGeneratePayload,
  PlatformContentUpdatePayload,
} from '../types/platformContent';

export function generatePlatformContents(articleId: number, data: PlatformContentGeneratePayload) {
  return request.post<unknown, ApiResult<ArticlePlatformContent[]>>(
    `/articles/${articleId}/platform-contents/generate`,
    data,
  );
}

export function getArticlePlatformContents(articleId: number) {
  return request.get<unknown, ApiResult<ArticlePlatformContent[]>>(
    `/articles/${articleId}/platform-contents`,
  );
}

export function getPlatformContentDetail(id: number) {
  return request.get<unknown, ApiResult<ArticlePlatformContent>>(`/platform-contents/${id}`);
}

export function updatePlatformContent(id: number, data: PlatformContentUpdatePayload) {
  return request.put<unknown, ApiResult<ArticlePlatformContent>>(`/platform-contents/${id}`, data);
}

export function archivePlatformContent(id: number) {
  return request.put<unknown, ApiResult<null>>(`/platform-contents/${id}/archive`);
}

export function restorePlatformContent(id: number) {
  return request.put<unknown, ApiResult<null>>(`/platform-contents/${id}/restore`);
}
