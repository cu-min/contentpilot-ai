import type { ArticleLanguage, ArticleType } from './article';

export interface AiArticleGenerateRequest {
  productConfigId?: number;
  topic: string;
  type: ArticleType;
  language: ArticleLanguage;
  extraRequirement?: string;
}

export type AiGenerationTaskStatus = 'PENDING' | 'SEARCHING' | 'WRITING' | 'SAVING' | 'SUCCESS' | 'FAILED';

export interface AiArticleGenerateSubmitResult {
  taskId: number;
  status: AiGenerationTaskStatus;
}

export interface AiGenerationTask {
  taskId: number;
  status: AiGenerationTaskStatus;
  progressMessage?: string;
  articleId?: number;
  errorMessage?: string;
  createdAt?: string;
  updatedAt?: string;
}
