import { describe, expect, it } from 'vitest';
import {
  getPreparedActionLabel,
  isHttpUrl,
  isPreparationReady,
  preparedFallbackUrls,
} from './publishPreparation';

describe('isPreparationReady', () => {
  const now = new Date('2026-07-16T10:00:00+08:00');

  it('allows immediate preparation', () => {
    expect(isPreparationReady('IMMEDIATE', undefined, now)).toBe(true);
  });

  it('blocks scheduled preparation before the configured time', () => {
    expect(isPreparationReady('SCHEDULED', '2026-07-16T10:30:00+08:00', now)).toBe(false);
  });

  it('allows scheduled preparation at or after the configured time', () => {
    expect(isPreparationReady('SCHEDULED', '2026-07-16T09:30:00+08:00', now)).toBe(true);
  });

  it('fails closed when a scheduled task has no valid time', () => {
    expect(isPreparationReady('SCHEDULED', undefined, now)).toBe(false);
    expect(isPreparationReady('SCHEDULED', 'invalid', now)).toBe(false);
  });
});

describe('manual confirmation actions', () => {
  it('shows the WeChat media id action when no draft URL is available', () => {
    expect(getPreparedActionLabel('WECHAT_OFFICIAL')).toBe('查看草稿信息');
  });

  it('shows editor actions for browser-assisted platforms', () => {
    expect(getPreparedActionLabel('CSDN')).toBe('打开人工确认窗口');
    expect(getPreparedActionLabel('ZHIHU')).toBe('打开人工确认窗口');
  });

  it('recognizes only HTTP links as directly openable draft URLs', () => {
    expect(isHttpUrl('https://example.com/draft/1')).toBe(true);
    expect(isHttpUrl('wechat-draft:media-id')).toBe(false);
    expect(preparedFallbackUrls.CSDN).toContain('csdn.net');
  });
});
