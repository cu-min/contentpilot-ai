import request, { ApiResult } from './request';

export function getHealth() {
  return request.get<unknown, ApiResult<string>>('/health');
}
