import type { PlatformContentPlatform } from '../types/platformContent';
import type { PublishType } from '../types/publishTask';

export const preparedFallbackUrls: Partial<Record<PlatformContentPlatform, string>> = {
  JUEJIN: 'https://juejin.cn/editor/drafts/new?v=2',
  CSDN: 'https://mp.csdn.net/mp_blog/manage/article',
  ZHIHU: 'https://www.zhihu.com/creator',
};

export function isPreparationReady(
  publishType: PublishType,
  scheduleTime?: string,
  now = new Date(),
) {
  if (publishType !== 'SCHEDULED') return true;
  if (!scheduleTime) return false;
  const scheduledAt = Date.parse(scheduleTime);
  return Number.isFinite(scheduledAt) && scheduledAt <= now.getTime();
}

export function isHttpUrl(value?: string) {
  return Boolean(value && /^https?:\/\//i.test(value));
}

export function getPreparedActionLabel(platform: PlatformContentPlatform, draftUrl?: string) {
  if (platform === 'WECHAT_OFFICIAL' && !isHttpUrl(draftUrl)) return '查看草稿信息';
  if (platform === 'CSDN' || platform === 'ZHIHU') return '打开人工确认窗口';
  return '打开草稿';
}
