import { useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import { authAPI } from '../api/auth';

export const useAuth = () => {
  const store = useAuthStore();

  useEffect(() => {
    store.loadFromStorage();
  }, []);

  const login = async (username: string, password: string) => {
    store.setLoading(true);
    store.setError(null);
    try {
      const response = await authAPI.login({ username, password });
      store.login(response.data);
      return true;
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Đăng nhập thất bại';
      store.setError(errorMessage);
      return false;
    } finally {
      store.setLoading(false);
    }
  };

  const logout = async () => {
    try {
      await authAPI.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      store.logout();
    }
  };

  return {
    ...store,
    login,
    logout,
  };
};
