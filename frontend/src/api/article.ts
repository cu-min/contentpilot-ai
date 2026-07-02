import request, { ApiResult } from './request';
import type {
  ArticleCreatePayload,
  ArticleDetail,
  ArticleListItem,
  ArticleQuery,
  ArticleUpdatePayload,
} from '../types/article';
import type { PageResult } from '../types';

export function getArticles(params: ArticleQuery) {
  return request.get<unknown, ApiResult<PageResult<ArticleListItem>>>('/articles', { params });
}

export function getArticleDetail(id: number) {
  return request.get<unknown, ApiResult<ArticleDetail>>(`/articles/${id}`);
}

export function createArticle(data: ArticleCreatePayload) {
  return request.post<unknown, ApiResult<ArticleDetail>>('/articles', data);
}

export function updateArticle(id: number, data: ArticleUpdatePayload) {
  return request.put<unknown, ApiResult<ArticleDetail>>(`/articles/${id}`, data);
}

export function archiveArticle(id: number) {
  return request.put<unknown, ApiResult<null>>(`/articles/${id}/archive`);
}

export function restoreArticle(id: number) {
  return request.put<unknown, ApiResult<null>>(`/articles/${id}/restore`);
}
