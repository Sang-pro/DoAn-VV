import { Product } from '../types';
import apiClient from './client';

export const getAllProducts = async (): Promise<Product[]> => {
    const response = await apiClient.get('/products');
    // If the backend returns a paginated response, the array is in response.data.content
    return response.data.content ? response.data.content : response.data;
};

export const createProduct = async (productData: Partial<Product>): Promise<Product> => {
    const response = await apiClient.post('/products', productData);
    return response.data;
};

export const updateProduct = async (id: number, productData: Partial<Product>): Promise<Product> => {
    const response = await apiClient.put(`/products/${id}`, productData);
    return response.data;
};

export const deleteProduct = async (id: number): Promise<void> => {
    const response = await apiClient.delete(`/products/${id}`);
    return response.data;
};
