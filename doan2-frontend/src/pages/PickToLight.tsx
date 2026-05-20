import React, { useEffect, useState } from 'react';
import { getAllBooks } from '../api/book';
import { pickToLight } from '../api/esltag';
import { Book } from '../types';

const PickToLight: React.FC = () => {
    const [books, setBooks] = useState<Book[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [statusMessage, setStatusMessage] = useState<string | null>(null);

    useEffect(() => {
        const fetchBooks = async () => {
            const data = await getAllBooks();
            setBooks(data || []);
        };
        fetchBooks();
    }, []);

    const handleFind = async (bookId: number) => {
        try {
            setStatusMessage(`Sending light signal via MQTT...`);
            const msg = await pickToLight(bookId);
            setStatusMessage(`✅ ${msg}`);
            setTimeout(() => setStatusMessage(null), 3000);
        } catch (err: any) {
            setStatusMessage(`❌ Error: ${err.response?.data || 'Failed to trigger light'}`);
        }
    };

    const filteredBooks = books.filter(b => 
        b.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
        b.isbn.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return (
        <div className="p-6">
            <h1 className="text-2xl font-bold mb-4">Pick To Light - Tìm Sách Nhanh</h1>
            <p className="mb-6 text-gray-600">Tìm kiếm sách và bấm "BẬT ĐÈN" để kích hoạt nhấp nháy đèn LED trên Nhãn kệ điện tử.</p>
            
            <input 
                type="text" 
                placeholder="Tìm theo tên hoặc ISBN..." 
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
                {filteredBooks.map(b => (
                    <div key={b.id} className="border p-4 rounded-lg shadow-sm bg-white hover:shadow-md transition">
                        <h3 className="text-xl font-bold">{b.title}</h3>
                        <p className="text-sm text-gray-500">ISBN: {b.isbn}</p>
                        <p className="text-sm">Có sẵn: <span className="font-bold">{b.availableCopies}</span> cuốn</p>
                        <button 
                            className="w-full mt-3 bg-red-600 hover:bg-red-700 text-white font-bold py-2 px-4 rounded transition"
                            onClick={() => b.id && handleFind(b.id)}
                        >
                            BẬT ĐÈN (TÌM SÁCH NÀY)
                        </button>
                    </div>
                ))}
                {filteredBooks.length === 0 && (
                    <div className="text-gray-500 col-span-full">Không tìm thấy sách phù hợp.</div>
                )}
            </div>
        </div>
    );
};

export default PickToLight;
