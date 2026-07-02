import { makeAutoObservable } from 'mobx';
import type { UserInfo } from '../types';

const TOKEN_KEY = 'ai_content_marketing_token';
const USER_KEY = 'ai_content_marketing_user';

class AuthStore {
  token = localStorage.getItem(TOKEN_KEY) || '';

  currentUser: UserInfo | null = this.readStoredUser();

  constructor() {
    makeAutoObservable(this);
  }

  get isLoggedIn() {
    return Boolean(this.token && this.currentUser);
  }

  get role() {
    return this.currentUser?.role;
  }

  setAuth(token: string, user: UserInfo) {
    this.token = token;
    this.currentUser = user;
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  setCurrentUser(user: UserInfo) {
    this.currentUser = user;
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  clearAuth() {
    this.token = '';
    this.currentUser = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  private readStoredUser() {
    const userText = localStorage.getItem(USER_KEY);
    if (!userText) {
      return null;
    }
    try {
      return JSON.parse(userText) as UserInfo;
    } catch {
      localStorage.removeItem(USER_KEY);
      return null;
    }
  }
}

export const authStore = new AuthStore();
export { TOKEN_KEY };
