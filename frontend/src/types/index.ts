export type PlatformType = 'WECHAT' | 'ZHIHU' | 'CSDN' | 'JUEJIN';

export type UserRole = 'ADMIN' | 'OPERATOR';

export interface UserInfo {
  id: number;
  username: string;
  nickname?: string;
  email?: string;
  role: UserRole;
  status: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginResponse {
  token: string;
  user: UserInfo;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
}
