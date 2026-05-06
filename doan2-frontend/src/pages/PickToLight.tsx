import React, { useEffect, useState, useCallback } from 'react';
import { getAllProducts } from '../api/product';
import { pickToLight } from '../api/esltag';
import { Product } from '../types';

const PickToLight: React.FC = () => {
    const [products, setProducts] = useState<Product[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [statusMessage, setStatusMessage] = useState<string | null>(null);

    useEffect(() => {
        const fetchProducts = async () => {
            const data = await getAllProducts();
            setProducts(data || []);
        };
        fetchProducts();
    }, []);

    const handleFind = async (productId: number) => {
        try {
            setStatusMessage(`Sending light signal via MQTT...`);
            const msg = await pickToLight(productId);
            setStatusMessage(`✅ ${msg}`);
            setTimeout(() => setStatusMessage(null), 3000);
        } catch (err: any) {
            setStatusMessage(`❌ Error: ${err.response?.data || 'Failed to trigger light'}`);
        }
    };

    const filteredProducts = products.filter(p => 
        p.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
        p.sku.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold mb-4">Pick To Light - Warehouse Find</h1>
            <p className="mb-6 text-gray-600">Search for a product below and click "Find" to trigger the blinking LED on the assigned Electronic Shelf Label.</p>
            
            <input 
                type="text" 
                placeholder="Search by name or sku..." 
                className="border p-2 w-full max-w-md mb-4 rounded"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
            />

            {statusMessage && (
                <div className="mb-4 p-3 bg-blue-50 text-blue-700 rounded border border-blue-200">
                    {statusMessage}
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mt-4">
                {filteredProducts.map(p => (
                    <div key={p.id} className="border p-4 rounded-lg shadow-sm bg-white hover:shadow-md transition">
                        <h3 className="text-xl font-bold">{p.name}</h3>
                        <p className="text-sm text-gray-500">SKU: {p.sku}</p>
                        <p className="text-sm">Stock: <span className="font-bold">{p.stockQuantity}</span></p>
                        <p className="text-sm">Location mapping assumes ESL is linked.</p>
                        <button 
                            className="w-full mt-3 bg-red-600 hover:bg-red-700 text-white font-bold py-2 px-4 rounded transition"
                            onClick={() => p.id && handleFind(p.id)}
                        >
                            FIND THIS ITEM (BLINK)
                        </button>
                    </div>
                ))}
                {filteredProducts.length === 0 && (
                    <div className="text-gray-500 col-span-full">No products found matching your search.</div>
                )}
            </div>
        </div>
    );
};

export default PickToLight;
