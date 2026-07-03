import request, { ApiResult } from './request';
import type { AiArticleGenerateRequest, AiArticleGenerateResult } from '../types/ai';

export function generateArticle(data: AiArticleGenerateRequest) {
  return request.post<unknown, ApiResult<AiArticleGenerateResult>>('/ai/articles/generate', data);
}
