import { Tag } from 'antd';
import type { ArticleLanguage, ArticleStatus, ArticleType } from '../../types/article';
import {
  getArticleLanguageLabel,
  getArticleStatusLabel,
  getArticleTypeLabel,
} from '../../types/article';

interface ArticleStatusTagProps {
  status: ArticleStatus;
}

interface ArticleTypeTagProps {
  type: ArticleType;
}

interface LanguageTagProps {
  language: ArticleLanguage;
}

const statusColors: Record<ArticleStatus, string> = {
  DRAFT: 'blue',
  PENDING: 'processing',
  PUBLISHED: 'success',
  FAILED: 'error',
  ARCHIVED: 'default',
};

export function StatusTag({ status }: ArticleStatusTagProps) {
  return <Tag color={statusColors[status]}>{getArticleStatusLabel(status)}</Tag>;
}

export function ArticleTypeTag({ type }: ArticleTypeTagProps) {
  return <Tag color="geekblue">{getArticleTypeLabel(type)}</Tag>;
}

export function LanguageTag({ language }: LanguageTagProps) {
  return <Tag color={language === 'ZH' ? 'purple' : 'cyan'}>{getArticleLanguageLabel(language)}</Tag>;
}
