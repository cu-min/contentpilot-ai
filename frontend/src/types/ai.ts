import type { ArticleLanguage, ArticleType } from './article';

export interface AiArticleGenerateRequest {
  topic: string;
  type: ArticleType;
  language: ArticleLanguage;
  extraRequirement?: string;
}

export interface AiArticleGenerateResult {
  articleId: number;
  title: string;
  summary?: string;
  content: string;
  type: ArticleType;
  language: ArticleLanguage;
  status: string;
  tags?: string;
  keywords?: string;
}
