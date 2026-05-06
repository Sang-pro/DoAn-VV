import apiClient from './client';
import { LoginRequest, JwtResponse, User, MessageResponse } from '../types';

export const authAPI = {
  login: (credentials: LoginRequest) =>
    apiClient.post<JwtResponse>('/auth/login', credentials),

  register: (data: any) =>
    apiClient.post<MessageResponse>('/auth/register', data),

  getCurrentUser: () =>
    apiClient.get<User>('/user-profiles/me'),

  logout: () =>
    apiClient.post<MessageResponse>('/auth/logout', {}),

  refreshToken: (refreshToken: string) =>
    apiClient.post<JwtResponse>('/auth/refresh', { refreshToken }),

  googleLogin: (clientId: string, token: string) =>
    apiClient.post<JwtResponse>('/auth/google', { clientId, token, provider: 'GOOGLE' }),

  facebookLogin: (clientId: string, token: string) =>
    apiClient.post<JwtResponse>('/auth/facebook', { clientId, token, provider: 'FACEBOOK' }),
};
