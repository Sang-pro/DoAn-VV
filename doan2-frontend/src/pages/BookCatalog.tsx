import React, { useEffect, useState, useCallback } from 'react';
import { getAllBooks, createBook, updateBook, deleteBook } from '../api/book';
import { Book } from '../types';
import { BookOpen, Edit, Trash2, Plus, Search } from 'lucide-react';
import { motion } from 'framer-motion';

const BookCatalog: React.FC = () => {
    const [books, setBooks] = useState<Book[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState('');

    const [isFormOpen, setIsFormOpen] = useState(false);
    const [selectedBook, setSelectedBook] = useState<Book | null>(null);
    const [formData, setFormData] = useState<Partial<Book>>({
        isbn: '', title: '', author: '', availableCopies: 0, summary: '', coverImageUrl: ''
    });

    const fetchBooks = useCallback(async () => {
        try {
            setLoading(true);
            const data = await getAllBooks();
            setBooks(data || []);
            setLoading(false);
        } catch (err) {
            console.error('Failed to fetch books:', err);
            setError('Failed to fetch books.');
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchBooks();
    }, [fetchBooks]);

    const handleCreate = () => {
        setSelectedBook(null);
        setFormData({ isbn: '', title: '', author: '', availableCopies: 0, summary: '', coverImageUrl: '' });
        setIsFormOpen(true);
    };

    const handleEdit = (book: Book) => {
        setSelectedBook(book);
        setFormData({
            isbn: book.isbn,
            title: book.title,
            author: book.author,
            availableCopies: book.availableCopies,
            summary: book.summary,
            coverImageUrl: book.coverImageUrl
        });
        setIsFormOpen(true);
    };

    const handleDelete = async (id: number) => {
        if (window.confirm("Bạn có chắc chắn muốn xóa cuốn sách này?")) {
            await deleteBook(id);
            fetchBooks();
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (selectedBook && selectedBook.id) {
            await updateBook(selectedBook.id, formData);
        } else {
            await createBook(formData);
        }
        setIsFormOpen(false);
        fetchBooks();
    };

    const filteredBooks = books.filter(b => 
        b.title.toLowerCase().includes(searchTerm.toLowerCase()) || 
        b.author.toLowerCase().includes(searchTerm.toLowerCase()) ||
        b.isbn.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="p-6 bg-gray-50 min-h-screen">
            <div className="flex justify-between items-end mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
                        <BookOpen className="text-indigo-600" size={32} />
                        Thư viện Sách
                    </h1>
                    <p className="text-gray-500 mt-2">Quản lý kho sách và đồng bộ dữ liệu với Nhãn giá điện tử</p>
                </div>
                
                <div className="flex gap-4 items-center">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                        <input 
                            type="text" 
                            placeholder="Tìm tên sách, tác giả..." 
                            className="pl-10 pr-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none w-64"
                            value={searchTerm}
                            onChange={e => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <button onClick={handleCreate} className="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-xl font-medium flex items-center gap-2 transition-colors shadow-sm">
                        <Plus size={20} />
                        Thêm Sách Mới
                    </button>
                </div>
            </div>

            {loading ? <div className="text-center py-20 text-gray-500">Đang tải dữ liệu sách...</div> : error ? <div className="text-red-500 text-center">{error}</div> : (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
                    {filteredBooks.map((book) => (
                        <motion.div 
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            key={book.id} 
                            className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-lg transition-all group relative flex flex-col"
                        >
                            {/* Actions Overlay */}
                            <div className="absolute top-3 right-3 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity z-10">
                                <button onClick={() => handleEdit(book)} className="p-2 bg-white/90 backdrop-blur text-indigo-600 rounded-full shadow hover:bg-indigo-50">
                                    <Edit size={16} />
                                </button>
                                <button onClick={() => book.id && handleDelete(book.id)} className="p-2 bg-white/90 backdrop-blur text-red-600 rounded-full shadow hover:bg-red-50">
                                    <Trash2 size={16} />
                                </button>
                            </div>

                            {/* Book Cover */}
                            <div className="h-64 bg-gray-100 w-full relative overflow-hidden flex items-center justify-center border-b border-gray-100">
                                {book.coverImageUrl ? (
                                    <img src={book.coverImageUrl} alt={book.title} className="w-full h-full object-cover" />
                                ) : (
                                    <BookOpen size={48} className="text-gray-300" />
                                )}
                                {book.availableCopies === 0 && (
                                    <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                                        <span className="bg-red-500 text-white px-3 py-1 rounded-full font-bold text-sm tracking-wide">HẾT SÁCH</span>
                                    </div>
                                )}
                            </div>

                            {/* Book Info */}
                            <div className="p-4 flex-1 flex flex-col">
                                <span className="text-xs font-mono text-gray-400 mb-1 block">ISBN: {book.isbn}</span>
                                <h3 className="font-bold text-gray-800 text-lg leading-tight mb-1 line-clamp-2">{book.title}</h3>
                                <p className="text-gray-500 text-sm mb-3 line-clamp-1">{book.author}</p>
                                
                                <div className="mt-auto flex justify-between items-center pt-3 border-t border-gray-50">
                                    <span className="text-xs text-gray-500">Tồn kho</span>
                                    <span className={`font-bold ${book.availableCopies > 0 ? 'text-green-600' : 'text-red-600'}`}>
                                        {book.availableCopies} cuốn
                                    </span>
                                </div>
                            </div>
                        </motion.div>
                    ))}
                </div>
            )}

            {isFormOpen && (
                <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50">
                    <motion.div 
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className="bg-white p-8 rounded-2xl w-[500px] shadow-2xl"
                    >
                        <h2 className="text-2xl font-bold mb-6 text-gray-800 flex items-center gap-2">
                            {selectedBook ? <Edit className="text-indigo-600"/> : <Plus className="text-indigo-600"/>}
                            {selectedBook ? 'Cập nhật Sách' : 'Thêm Sách Mới'}
                        </h2>
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Mã ISBN</label>
                                    <input required className="w-full border border-gray-300 rounded-xl p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none bg-gray-50" value={formData.isbn} onChange={e => setFormData({...formData, isbn: e.target.value})} />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Số lượng tồn</label>
                                    <input required type="number" min="0" className="w-full border border-gray-300 rounded-xl p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none bg-gray-50" value={formData.availableCopies} onChange={e => setFormData({...formData, availableCopies: Number(e.target.value)})} />
                                </div>
                            </div>
                            
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Tên Sách</label>
                                <input required className="w-full border border-gray-300 rounded-xl p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none bg-gray-50" value={formData.title} onChange={e => setFormData({...formData, title: e.target.value})} />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Tác giả</label>
                                <input required className="w-full border border-gray-300 rounded-xl p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none bg-gray-50" value={formData.author} onChange={e => setFormData({...formData, author: e.target.value})} />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Link Ảnh Bìa (Cover Image URL)</label>
                                <input className="w-full border border-gray-300 rounded-xl p-2.5 focus:ring-2 focus:ring-indigo-500 outline-none bg-gray-50" value={formData.coverImageUrl || ''} onChange={e => setFormData({...formData, coverImageUrl: e.target.value})} placeholder="https://example.com/cover.jpg" />
                            </div>

                            <div className="flex justify-end gap-3 mt-8">
                                <button type="button" onClick={() => setIsFormOpen(false)} className="px-5 py-2.5 rounded-xl font-medium text-gray-600 hover:bg-gray-100 transition-colors">Hủy bỏ</button>
                                <button type="submit" className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2.5 rounded-xl font-medium transition-colors shadow-sm">Lưu & Đồng bộ Nhãn giá</button>
                            </div>
                        </form>
                    </motion.div>
                </div>
            )}
        </div>
    );
};

export default BookCatalog;
