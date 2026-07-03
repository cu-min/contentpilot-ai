import type { PlatformContentPlatform } from './platformContent';

export type PlatformAuthType = 'APP_SECRET' | 'COOKIE' | 'BROWSER_PROFILE' | 'API_KEY' | 'MANUAL';

export type PublishMode = 'OFFICIAL_API' | 'UNOFFICIAL_API' | 'BROWSER_AUTOMATION' | 'MANUAL_CONFIRM';

export interface PlatformAccount {
  id: number;
  platform: PlatformContentPlatform;
  accountName: string;
  authType: PlatformAuthType;
  authConfigConfigured: boolean;
  authConfigMasked?: string;
  defaultPublishMode: PublishMode;
  enabled: number;
  remark?: string;
  createdBy?: number;
  updatedBy?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PlatformAccountQuery {
  platform?: PlatformContentPlatform;
  enabled?: number;
}

export interface PlatformAccountPayload {
  platform: PlatformContentPlatform;
  accountName: string;
  authType: PlatformAuthType;
  authConfig?: string;
  defaultPublishMode: PublishMode;
  enabled: number;
  remark?: string;
}

export const authTypeOptions = [
  { label: 'App Secret', value: 'APP_SECRET' },
  { label: 'Cookie', value: 'COOKIE' },
  { label: '浏览器 Profile', value: 'BROWSER_PROFILE' },
  { label: 'API Key', value: 'API_KEY' },
  { label: '人工维护', value: 'MANUAL' },
] as const;

export const publishModeOptions = [
  { label: '官方 API', value: 'OFFICIAL_API' },
  { label: '非官方 API', value: 'UNOFFICIAL_API' },
  { label: '浏览器自动化', value: 'BROWSER_AUTOMATION' },
  { label: '人工确认', value: 'MANUAL_CONFIRM' },
] as const;

export function getAuthTypeLabel(type?: string) {
  return authTypeOptions.find((option) => option.value === type)?.label || type || '-';
}

export function getPublishModeLabel(mode?: string) {
  return publishModeOptions.find((option) => option.value === mode)?.label || mode || '-';
}
