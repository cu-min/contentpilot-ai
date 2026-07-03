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

export const platformContentRuleSummaries = [
  {
    platform: 'WECHAT_OFFICIAL',
    name: '微信公众号',
    goal: '品牌内容、产品传播、移动端阅读',
    summary: '保留核心痛点、产品价值和场景案例，删减过长技术细节和硬销售话术。',
  },
  {
    platform: 'ZHIHU',
    name: '知乎',
    goal: '观点分析、问题回答、经验分享',
    summary: '保留问题背景、逻辑推导和客观建议，弱化产品自夸和直接转化话术。',
  },
  {
    platform: 'CSDN',
    name: 'CSDN',
    goal: '技术教程、实践说明、开发者学习',
    summary: '保留技术背景、步骤、示例和配置说明，删除品牌故事和运营化表达。',
  },
  {
    platform: 'JUEJIN',
    name: '掘金',
    goal: '开发者经验、技术实践、效率工具分享',
    summary: '保留开发者痛点、实践经验和方法论，删减官方宣传和纯概念段落。',
  },
] as const satisfies Array<{
  platform: PlatformContentPlatform;
  name: string;
  goal: string;
  summary: string;
}>;

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
