import React, { useEffect, useState, useCallback } from 'react';
import { getAllTags, createTag, updateTag, deleteTag } from '../api/esltag';
import { getAllProducts } from '../api/product';
import { EslTag, Product } from '../types';

const EslManagement: React.FC = () => {
    const [tags, setTags] = useState<EslTag[]>([]);
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(true);

    const [isFormOpen, setIsFormOpen] = useState(false);
    const [selectedTag, setSelectedTag] = useState<EslTag | null>(null);
    const [formData, setFormData] = useState<Partial<EslTag>>({
        macAddress: '', location: '', batteryLevel: 100, isOnline: true, status: 'Active'
    });
    const [selectedProductId, setSelectedProductId] = useState<number | ''>('');

    const fetchData = useCallback(async () => {
        setLoading(true);
        const [tagsData, productsData] = await Promise.all([getAllTags(), getAllProducts()]);
        setTags(tagsData || []);
        setProducts(productsData || []);
        setLoading(false);
    }, []);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const handleCreate = () => {
        setSelectedTag(null);
        setFormData({ macAddress: '', location: '', batteryLevel: 100, isOnline: true, status: 'Active' });
        setSelectedProductId('');
        setIsFormOpen(true);
    };

    const handleEdit = (tag: EslTag) => {
        setSelectedTag(tag);
        setFormData(tag);
        setSelectedProductId(tag.product?.id || '');
        setIsFormOpen(true);
    };

    const handleDelete = async (id: number) => {
        if (window.confirm("Delete this ESL Tag?")) {
            await deleteTag(id);
            fetchData();
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        const payload = { ...formData };
        if (selectedProductId) {
            payload.product = { id: Number(selectedProductId) } as Product;
        } else {
            payload.product = undefined;
        }

        if (selectedTag && selectedTag.id) {
            await updateTag(selectedTag.id, payload);
        } else {
            await createTag(payload);
        }
        setIsFormOpen(false);
        fetchData();
    };

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold mb-4">ESL Device Management</h1>
            <div className="flex justify-between items-center mb-4">
                <button onClick={handleCreate} className="bg-blue-500 text-white p-2 rounded">Add ESL Tag</button>
            </div>

            {loading ? <div>Loading...</div> : (
                <div className="bg-white shadow-md rounded-lg p-4 overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">MAC Address</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Location</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Battery</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Online</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Linked Product</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {tags.map((t) => (
                                <tr key={t.id}>
                                    <td className="px-6 py-4 whitespace-nowrap">{t.macAddress}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">{t.location}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">{t.batteryLevel}%</td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        {t.isOnline ? <span className="text-green-500">Online</span> : <span className="text-gray-500">Offline</span>}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">{t.product?.name || <span className="text-gray-400">Unassigned</span>}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <button onClick={() => handleEdit(t)} className="text-indigo-600 hover:text-indigo-900 mr-2">Edit</button>
                                        <button onClick={() => t.id && handleDelete(t.id)} className="text-red-600 hover:text-red-900">Delete</button>
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
                        <h2 className="text-xl font-bold mb-4">{selectedTag ? 'Edit Tag' : 'Add Tag'}</h2>
                        <form onSubmit={handleSubmit}>
                            <div className="mb-2">
                                <label className="block text-sm font-medium">MAC Address</label>
                                <input required className="w-full border p-1" value={formData.macAddress} onChange={e => setFormData({...formData, macAddress: e.target.value})} />
                            </div>
                            <div className="mb-2">
                                <label className="block text-sm font-medium">Location</label>
                                <input className="w-full border p-1" value={formData.location || ''} onChange={e => setFormData({...formData, location: e.target.value})} />
                            </div>
                            <div className="mb-2">
                                <label className="block text-sm font-medium">Battery Level (%)</label>
                                <input type="number" className="w-full border p-1" value={formData.batteryLevel} onChange={e => setFormData({...formData, batteryLevel: Number(e.target.value)})} />
                            </div>
                            <div className="mb-2">
                                <label className="block text-sm font-medium items-center flex">
                                    <input type="checkbox" className="mr-2" checked={formData.isOnline} onChange={e => setFormData({...formData, isOnline: e.target.checked})} />
                                    Online Status
                                </label>
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium">Link Product</label>
                                <select className="w-full border p-1" value={selectedProductId} onChange={e => setSelectedProductId(e.target.value ? Number(e.target.value) : '')}>
                                    <option value="">-- No Product --</option>
                                    {products.map(p => <option key={p.id} value={p.id}>{p.name} (SKU: {p.sku})</option>)}
                                </select>
                            </div>
                            <div className="flex justify-end gap-2">
                                <button type="button" onClick={() => setIsFormOpen(false)} className="bg-gray-300 px-4 py-2 rounded">Cancel</button>
                                <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded">Save</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default EslManagement;
