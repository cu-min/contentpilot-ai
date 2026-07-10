export type GrowthTrackingPlatform =
  | 'WECHAT_OFFICIAL'
  | 'ZHIHU'
  | 'CSDN'
  | 'JUEJIN'
  | 'OTHER';

export type GrowthCheckStatus = 'SUCCESS' | 'FAILED' | 'UNKNOWN';

export interface GrowthTrackingTarget {
  id: number;
  name: string;
  platform: GrowthTrackingPlatform;
  platformLabel: string;
  targetUrl: string;
  remark?: string | null;
  enabled: number;
  lastCheckStatus: GrowthCheckStatus;
  lastHttpStatus?: number | null;
  lastPageTitle?: string | null;
  lastErrorMessage?: string | null;
  lastCheckedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface GrowthTrackingTargetPayload {
  name: string;
  platform: GrowthTrackingPlatform;
  targetUrl: string;
  remark?: string;
  enabled: number;
}

export interface GrowthTrackingTargetQuery {
  name?: string;
  platform?: GrowthTrackingPlatform;
  enabled?: number;
}

export const growthTrackingPlatformOptions = [
  { label: '微信公众号', value: 'WECHAT_OFFICIAL' },
  { label: '知乎', value: 'ZHIHU' },
  { label: 'CSDN', value: 'CSDN' },
  { label: '掘金', value: 'JUEJIN' },
  { label: '其他', value: 'OTHER' },
] as const;

export function getGrowthCheckStatusLabel(status?: string) {
  if (status === 'SUCCESS') return '可访问';
  if (status === 'FAILED') return '异常';
  return '未查询';
}
