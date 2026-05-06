import React, { useEffect, useState } from 'react';
import { getAllTags, pickToLight } from '../api/esltag';
import { EslTag } from '../types';

export const ShelfMap: React.FC = () => {
    const [shelves, setShelves] = useState<EslTag[]>([]);
    const [loading, setLoading] = useState(true);

    const fetchShelves = async () => {
        try {
            const data = await getAllTags();
            // Filter tags that have a location (representing physical shelves)
            setShelves(data?.filter(t => t.location) || []);
        } catch (error) {
            console.error("Error fetching shelf data:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchShelves();
        // Polling every 3 seconds for real-time updates (Digital Twin)
        const interval = setInterval(fetchShelves, 3000);
        return () => clearInterval(interval);
    }, []);

    const handlePickToLight = async (productId: number) => {
        try {
            await pickToLight(productId);
            alert("Đã gửi lệnh bật đèn (Pick-to-light) thành công!");
        } catch (err) {
            alert("Lỗi khi gửi lệnh bật đèn.");
        }
    };

    return (
        <div className="p-6">
            <h1 className="text-3xl font-bold mb-2">Bản đồ Quầy hàng (Digital Twin)</h1>
            <p className="text-gray-500 mb-6">Theo dõi vị trí sản phẩm trên các quầy kệ theo thời gian thực nhờ công nghệ RFID.</p>

            {loading ? (
                <div className="text-center py-10 text-gray-500">Đang tải bản đồ...</div>
            ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                    {shelves.length === 0 && (
                        <div className="col-span-full text-center py-10 text-gray-400">
                            Chưa có quầy kệ nào được thiết lập vị trí. Vui lòng thêm ESL Tag và gán Location (vd: A1).
                        </div>
                    )}
                    
                    {shelves.map((shelf) => (
                        <div 
                            key={shelf.id} 
                            className={`border-2 rounded-xl p-4 shadow-sm flex flex-col items-center text-center transition-all duration-500 ${shelf.product ? 'border-indigo-400 bg-indigo-50/50' : 'border-dashed border-gray-300 bg-gray-50'}`}
                        >
                            <div className="bg-gray-800 text-white font-mono text-xl font-bold px-4 py-1 rounded-md mb-4 w-full">
                                Vị trí: {shelf.location}
                            </div>
                            
                            {shelf.product ? (
                                <>
                                    <div className="w-20 h-20 bg-white rounded-full flex items-center justify-center shadow-inner mb-3 border border-indigo-100">
                                        <span className="text-3xl text-indigo-500">📦</span>
                                    </div>
                                    <h3 className="font-bold text-lg text-gray-800 line-clamp-1">{shelf.product.name}</h3>
                                    <p className="text-sm text-gray-500 mb-1">SKU: {shelf.product.sku}</p>
                                    <p className="text-indigo-600 font-semibold mb-4">Tồn kho: {shelf.product.stockQuantity}</p>
                                    
                                    <button 
                                        onClick={() => shelf.product?.id && handlePickToLight(shelf.product.id)}
                                        className="mt-auto w-full bg-yellow-400 hover:bg-yellow-500 text-yellow-900 font-bold py-2 px-4 rounded-lg flex items-center justify-center gap-2 transition-colors shadow-sm"
                                    >
                                        <span>💡 Bật đèn (Lấy hàng)</span>
                                    </button>
                                </>
                            ) : (
                                <div className="flex-1 flex flex-col items-center justify-center py-6">
                                    <div className="w-16 h-16 border-2 border-dashed border-gray-300 rounded-full flex items-center justify-center mb-3">
                                        <span className="text-gray-300 text-2xl">?</span>
                                    </div>
                                    <p className="text-gray-400 font-medium">Trống</p>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ShelfMap;
