import React, { useEffect, useState, useCallback } from 'react';
import { getAllProducts, createProduct, updateProduct, deleteProduct } from '../api/product';
import { Product } from '../types';

const ProductManagement: React.FC = () => {
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [isFormOpen, setIsFormOpen] = useState(false);
    const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
    const [formData, setFormData] = useState<Partial<Product>>({
        sku: '', name: '', price: 0, stockQuantity: 0, description: '', qrCodeUrl: ''
    });

    const fetchProducts = useCallback(async () => {
        try {
            setLoading(true);
            const data = await getAllProducts();
            setProducts(data || []);
            setLoading(false);
        } catch (err) {
            console.error('Failed to fetch products:', err);
            setError('Failed to fetch products.');
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchProducts();
    }, [fetchProducts]);

    const handleCreate = () => {
        setSelectedProduct(null);
        setFormData({ sku: '', name: '', price: 0, stockQuantity: 0, description: '', qrCodeUrl: '' });
        setIsFormOpen(true);
    };

    const handleEdit = (product: Product) => {
        setSelectedProduct(product);
        setFormData(product);
        setIsFormOpen(true);
    };

    const handleDelete = async (id: number) => {
        if (window.confirm("Are you sure you want to delete this product?")) {
            await deleteProduct(id);
            fetchProducts();
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (selectedProduct && selectedProduct.id) {
            await updateProduct(selectedProduct.id, formData);
        } else {
            await createProduct(formData);
        }
        setIsFormOpen(false);
        fetchProducts();
    };

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold mb-4">Product Management</h1>
            <div className="flex justify-between items-center mb-4">
                <button onClick={handleCreate} className="bg-blue-500 text-white p-2 rounded">
                    Add Product
                </button>
            </div>

            {loading ? <div>Loading...</div> : error ? <div className="text-red-500">{error}</div> : (
                <div className="bg-white shadow-md rounded-lg p-4 overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Price</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Stock</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {products.map((p) => (
                                <tr key={p.id}>
                                    <td className="px-6 py-4 whitespace-nowrap">{p.sku}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">{p.name}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">{p.price.toLocaleString()}đ</td>
                                    <td className="px-6 py-4 whitespace-nowrap">{p.stockQuantity}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        {p.stockQuantity === 0 ? <span className="text-red-500 font-bold">Sold Out</span> : <span className="text-green-500">In Stock</span>}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <button onClick={() => handleEdit(p)} className="text-indigo-600 hover:text-indigo-900 mr-2">Edit</button>
                                        <button onClick={() => p.id && handleDelete(p.id)} className="text-red-600 hover:text-red-900">Delete</button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            {isFormOpen && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white p-6 rounded-lg w-96">
                        <h2 className="text-xl font-bold mb-4">{selectedProduct ? 'Edit Product' : 'Add Product'}</h2>
                        <form onSubmit={handleSubmit}>
                            <div className="mb-2">
                                <label className="block text-sm font-medium">SKU</label>
                                <input required className="w-full border p-1" value={formData.sku} onChange={e => setFormData({...formData, sku: e.target.value})} />
                            </div>
                            <div className="mb-2">
                                <label className="block text-sm font-medium">Name</label>
                                <input required className="w-full border p-1" value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} />
                            </div>
                            <div className="mb-2">
                                <label className="block text-sm font-medium">Price</label>
                                <input required type="number" className="w-full border p-1" value={formData.price} onChange={e => setFormData({...formData, price: Number(e.target.value)})} />
                            </div>
                            <div className="mb-2">
                                <label className="block text-sm font-medium">Stock Quantity</label>
                                <input required type="number" className="w-full border p-1" value={formData.stockQuantity} onChange={e => setFormData({...formData, stockQuantity: Number(e.target.value)})} />
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium">QR/URL Content</label>
                                <input className="w-full border p-1" value={formData.qrCodeUrl || ''} onChange={e => setFormData({...formData, qrCodeUrl: e.target.value})} />
                            </div>
                            <div className="flex justify-end gap-2">
                                <button type="button" onClick={() => setIsFormOpen(false)} className="bg-gray-300 px-4 py-2 rounded">Cancel</button>
                                <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded">Save & Sync</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ProductManagement;
