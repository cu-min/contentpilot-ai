export type PlatformContentPlatform = 'WECHAT_OFFICIAL' | 'ZHIHU' | 'CSDN' | 'JUEJIN';

export type PlatformContentStatus = 'DRAFT' | 'ARCHIVED';

export interface ArticlePlatformContent {
  id: number;
  articleId: number;
  platform: PlatformContentPlatform;
  title: string;
  summary?: string;
  content?: string;
  tags?: string;
  keywords?: string;
  status: PlatformContentStatus;
  createdBy?: number;
  updatedBy?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PlatformContentGeneratePayload {
  platforms: PlatformContentPlatform[];
  extraRequirement?: string;
}

export interface PlatformContentUpdatePayload {
  title: string;
  summary?: string;
  content?: string;
  tags?: string;
  keywords?: string;
}

export const platformContentOptions = [
  { label: '微信公众号', value: 'WECHAT_OFFICIAL' },
  { label: '知乎', value: 'ZHIHU' },
  { label: 'CSDN', value: 'CSDN' },
  { label: '掘金', value: 'JUEJIN' },
] as const;

export function getPlatformContentPlatformLabel(platform?: string) {
  return platformContentOptions.find((option) => option.value === platform)?.label || platform || '-';
}

export function getPlatformContentStatusLabel(status?: string) {
  if (status === 'DRAFT') {
    return '草稿';
  }
  if (status === 'ARCHIVED') {
    return '已归档';
  }
  return status || '-';
}
