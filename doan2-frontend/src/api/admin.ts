import { User, CreateUser, UpdateUser, UserStatusUpdate } from '../types';
import apiClient from './client';

export const listUsers = async (search: string, isActive: boolean, page: number, size: number): Promise<any> => {
    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (isActive !== null) params.append('isActive', String(isActive));
    if (page) params.append('page', String(page));
    if (size) params.append('size', String(size));
    const response = await apiClient.get(`/admin/users?${params.toString()}`);
    return response.data;
};

export const getUserDetails = async (userId: string): Promise<User> => {
    const response = await apiClient.get(`/admin/users/${userId}`);
    return response.data;
};

export const createUser = async (userData: CreateUser): Promise<User> => {
    const response = await apiClient.post('/admin/users', userData);
    return response.data;
};

export const updateUser = async (userId: string, userData: UpdateUser): Promise<User> => {
    const response = await apiClient.put(`/admin/users/${userId}`, userData);
    return response.data;
};

export const updateUserStatus = async (userId: string, status: UserStatusUpdate): Promise<{ message: string, success: boolean }> => {
    const response = await apiClient.patch(`/admin/users/${userId}/status`, status);
    return response.data;
};

export const deleteUser = async (userId: string): Promise<{ message: string, success: boolean }> => {
    const response = await apiClient.delete(`/admin/users/${userId}`);
    return response.data;
};
