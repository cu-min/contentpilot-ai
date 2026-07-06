export function getErrorText(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

export function formatFailure(action: string, error: unknown, fallback = '请稍后重试') {
  return `${action}失败：${getErrorText(error, fallback)}`;
}
