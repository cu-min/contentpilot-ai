import request, { ApiResult } from './request';
import type { AiArticleGenerateRequest, AiArticleGenerateSubmitResult, AiGenerationTask } from '../types/ai';

export function generateArticle(data: AiArticleGenerateRequest) {
  return request.post<unknown, ApiResult<AiArticleGenerateSubmitResult>>('/ai/articles/generate', data);
}

export function getAiGenerationTask(taskId: number) {
  return request.get<unknown, ApiResult<AiGenerationTask>>(`/ai/generation-tasks/${taskId}`);
}
