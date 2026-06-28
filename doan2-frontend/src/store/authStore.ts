import { create } from 'zustand';
import { JwtResponse, User } from '../types';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  setUser: (user: User | null) => void;
  setAccessToken: (token: string | null) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  login: (response: JwtResponse) => void;
  logout: () => void;
  loadFromStorage: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,

  setUser: (user) => set({ user }),
  setAccessToken: (token) => set({ accessToken: token }),
  setLoading: (isLoading) => set({ isLoading }),
  setError: (error) => set({ error }),

  login: (response: JwtResponse) => {
    const user: User = {
      id: response.id || '',
      username: response.username,
      email: response.email || '',
      isActive: true,
      roles: response.roles || [],
      userCode: response.userCode || '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('user', JSON.stringify(user));
    set({
      user,
      accessToken: response.accessToken,
      isAuthenticated: true,
      error: null,
    });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    set({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      error: null,
    });
  },

  loadFromStorage: () => {
    const token = localStorage.getItem('accessToken');
    const userJson = localStorage.getItem('user');
    if (token && userJson) {
      try {
        const user = JSON.parse(userJson);
        set({
          accessToken: token,
          user,
          isAuthenticated: true,
        });
      } catch (e) {
        localStorage.removeItem('user');
        localStorage.removeItem('accessToken');
      }
    }
  },
}));
