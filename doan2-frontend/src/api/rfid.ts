import { RfidTag } from '../types';
import apiClient from './client';

export const getAllRfidTags = async (): Promise<RfidTag[]> => {
    const response = await apiClient.get('/rfid');
    return response.data;
};

export const registerRfidTag = async (epc: string, bookId: number): Promise<RfidTag> => {
    const response = await apiClient.post('/rfid/register', { epc, bookId });
    return response.data;
};

export const deleteRfidTag = async (id: number): Promise<void> => {
    const response = await apiClient.delete(`/rfid/${id}`);
    return response.data;
};
