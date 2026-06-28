import { BorrowRecord } from '../types';
import apiClient from './client';

export const borrowBook = async (bookId: number, userCode: string): Promise<BorrowRecord> => {
  const response = await apiClient.post<BorrowRecord>('/borrow/borrow', { bookId, userCode });
  return response.data;
};

export const returnBook = async (bookId: number, userCode: string): Promise<BorrowRecord> => {
  const response = await apiClient.post<BorrowRecord>('/borrow/return', { bookId, userCode });
  return response.data;
};

export const getHistoryByUser = async (userCode: string): Promise<BorrowRecord[]> => {
  const response = await apiClient.get<BorrowRecord[]>(`/borrow/user/${userCode}`);
  return response.data;
};

export const getAllRecords = async (): Promise<BorrowRecord[]> => {
  const response = await apiClient.get<BorrowRecord[]>('/borrow/all');
  return response.data;
};
