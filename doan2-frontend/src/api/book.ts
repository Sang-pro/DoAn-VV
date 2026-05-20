import { Book } from '../types';
import apiClient from './client';

export const getAllBooks = async (): Promise<Book[]> => {
    const response = await apiClient.get('/books');
    // If the backend returns a paginated response, the array is in response.data.content
    return response.data.content ? response.data.content : response.data;
};

export const createBook = async (bookData: Partial<Book>): Promise<Book> => {
    const response = await apiClient.post('/books', bookData);
    return response.data;
};

export const updateBook = async (id: number, bookData: Partial<Book>): Promise<Book> => {
    const response = await apiClient.put(`/books/${id}`, bookData);
    return response.data;
};

export const deleteBook = async (id: number): Promise<void> => {
    const response = await apiClient.delete(`/books/${id}`);
    return response.data;
};
