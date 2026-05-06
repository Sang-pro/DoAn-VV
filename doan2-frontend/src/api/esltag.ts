import { EslTag } from '../types';
import apiClient from './client';

export const getAllTags = async (): Promise<EslTag[]> => {
    const response = await apiClient.get('/esl');
    return response.data;
};

export const createTag = async (tagData: Partial<EslTag>): Promise<EslTag> => {
    const response = await apiClient.post('/esl', tagData);
    return response.data;
};

export const updateTag = async (id: number, tagData: Partial<EslTag>): Promise<EslTag> => {
    const response = await apiClient.put(`/esl/${id}`, tagData);
    return response.data;
};

export const deleteTag = async (id: number): Promise<void> => {
    const response = await apiClient.delete(`/esl/${id}`);
    return response.data;
};

export const pickToLight = async (productId: number): Promise<string> => {
    const response = await apiClient.post(`/esl/find/${productId}`);
    return response.data;
};
