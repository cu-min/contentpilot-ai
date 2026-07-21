import { Tag } from 'antd';
import type { ArticleStatus, ArticleType } from '../../types/article';
import {
  getArticleStatusLabel,
  getArticleTypeLabel,
} from '../../types/article';

interface ArticleStatusTagProps {
  status: ArticleStatus;
}

interface ArticleTypeTagProps {
  type: ArticleType;
}

const statusColors: Record<ArticleStatus, string> = {
  DRAFT: 'blue',
  PENDING: 'processing',
  PUBLISHED: 'success',
  FAILED: 'error',
  ARCHIVED: 'default',
};

const typeColors: Record<ArticleType, string> = {
  PRODUCT_INTRO: 'blue',
  TUTORIAL: 'cyan',
  INDUSTRY_KNOWLEDGE: 'green',
  COMPARISON: 'purple',
  SOLUTION: 'orange',
  SEO: 'geekblue',
};

export function StatusTag({ status }: ArticleStatusTagProps) {
  return <Tag color={statusColors[status]}>{getArticleStatusLabel(status)}</Tag>;
}

export function ArticleTypeTag({ type }: ArticleTypeTagProps) {
  return <Tag color={typeColors[type]}>{getArticleTypeLabel(type)}</Tag>;
}

