export type ArticleType =
  | 'PRODUCT_INTRO'
  | 'TUTORIAL'
  | 'INDUSTRY_KNOWLEDGE'
  | 'COMPARISON'
  | 'SOLUTION'
  | 'SEO';

export type ArticleLanguage = 'ZH' | 'EN';

export type ArticleStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'FAILED' | 'ARCHIVED';

export interface ArticleQuery {
  page: number;
  size: number;
  keyword?: string;
  status?: ArticleStatus;
  type?: ArticleType;
  language?: ArticleLanguage;
}

export interface ArticleListItem {
  id: number;
  title: string;
  summary?: string;
  type: ArticleType;
  language: ArticleLanguage;
  status: ArticleStatus;
  tags?: string;
  createdBy?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ArticleDetail extends ArticleListItem {
  content?: string;
  keywords?: string;
  updatedBy?: number;
}

export interface ArticleCreatePayload {
  title: string;
  summary?: string;
  content?: string;
  type: ArticleType;
  language: ArticleLanguage;
  tags?: string;
  keywords?: string;
}

export type ArticleUpdatePayload = ArticleCreatePayload;

export const articleTypeOptions = [
  { label: '产品介绍', value: 'PRODUCT_INTRO' },
  { label: '使用教程', value: 'TUTORIAL' },
  { label: '行业科普', value: 'INDUSTRY_KNOWLEDGE' },
  { label: '竞品对比', value: 'COMPARISON' },
  { label: '解决方案', value: 'SOLUTION' },
  { label: 'SEO文章', value: 'SEO' },
] as const;

export const articleLanguageOptions = [
  { label: '中文', value: 'ZH' },
  { label: '英文', value: 'EN' },
] as const;

export const articleStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待发布', value: 'PENDING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '发布失败', value: 'FAILED' },
  { label: '已归档', value: 'ARCHIVED' },
] as const;

export function getArticleTypeLabel(type?: string) {
  return articleTypeOptions.find((option) => option.value === type)?.label || type || '-';
}

export function getArticleLanguageLabel(language?: string) {
  return articleLanguageOptions.find((option) => option.value === language)?.label || language || '-';
}

export function getArticleStatusLabel(status?: string) {
  return articleStatusOptions.find((option) => option.value === status)?.label || status || '-';
}
