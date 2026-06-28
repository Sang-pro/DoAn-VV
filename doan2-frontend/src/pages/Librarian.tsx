import React, { useState, useEffect, useRef } from 'react';
import { Search, Save, BookOpen, Tag, Hash, Barcode, CheckCircle, UserCheck, RotateCcw, Calendar, Clock, AlertTriangle, ArrowRightLeft } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { getAllBooks, updateBook } from '../api/book';
import { borrowBook, returnBook, getAllRecords } from '../api/borrow';
import { Book, BorrowRecord } from '../types';

export const Librarian: React.FC = () => {
  const [isbnInput, setIsbnInput] = useState('');
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [scannedBook, setScannedBook] = useState<Book | null>(null);
  
  // Tabs: 'borrow' | 'return' | 'edit'
  const [activeTab, setActiveTab] = useState<'borrow' | 'return' | 'edit'>('borrow');
  const [userCodeInput, setUserCodeInput] = useState('');
  const [borrowLoading, setBorrowLoading] = useState(false);
  const [borrowSuccess, setBorrowSuccess] = useState('');
  const [borrowError, setBorrowError] = useState('');

  // Form state for editing
  const [editForm, setEditForm] = useState<Partial<Book>>({
    title: '',
    author: '',
    availableCopies: 0,
  });
  
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  
  // Recent Records
  const [recentRecords, setRecentRecords] = useState<BorrowRecord[]>([]);
  const [recordsLoading, setRecordsLoading] = useState(false);
  
  const inputRef = useRef<HTMLInputElement>(null);

  const fetchRecentRecords = async () => {
    try {
      setRecordsLoading(true);
      const data = await getAllRecords();
      // Sort by date/time (most recent first)
      const sorted = (data || []).sort((a, b) => {
        const dateA = new Date(a.borrowDate || 0).getTime();
        const dateB = new Date(b.borrowDate || 0).getTime();
        return dateB - dateA;
      });
      setRecentRecords(sorted);
    } catch (err) {
      console.error('Failed to fetch recent records:', err);
    } finally {
      setRecordsLoading(false);
    }
  };

  useEffect(() => {
    const fetchBooks = async () => {
      try {
        setLoading(true);
        const data = await getAllBooks();
        setBooks(data || []);
      } catch (err) {
        console.error('Failed to fetch books:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchBooks();
    fetchRecentRecords();
    
    if (inputRef.current) {
      inputRef.current.focus();
    }
  }, []);

  const handleScan = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!isbnInput.trim()) return;

    setSearchError('');
    setSaveSuccess(false);
    setBorrowError('');
    setBorrowSuccess('');

    const found = books.find(b => b.isbn === isbnInput.trim() || b.isbn === isbnInput.trim().toUpperCase());
    
    if (found) {
      setScannedBook(found);
      setEditForm({
        title: found.title,
        author: found.author,
        availableCopies: found.availableCopies,
      });
      setIsbnInput('');
    } else {
      setScannedBook(null);
      setSearchError('Không tìm thấy sách với mã ISBN này!');
    }
  };

  const handleSave = async () => {
    if (!scannedBook || !scannedBook.id) return;
    
    try {
      setIsSaving(true);
      setSaveSuccess(false);
      const updated = await updateBook(scannedBook.id, {
        isbn: scannedBook.isbn,
        summary: scannedBook.summary,
        coverImageUrl: scannedBook.coverImageUrl,
        title: editForm.title,
        author: editForm.author,
        availableCopies: editForm.availableCopies,
      });
      
      setBooks(books.map(b => b.id === updated.id ? updated : b));
      setScannedBook(updated);
      setSaveSuccess(true);
      
      setTimeout(() => setSaveSuccess(false), 3000);
      
      if (inputRef.current) {
        inputRef.current.focus();
      }
    } catch (err) {
      console.error('Lỗi khi cập nhật sách:', err);
      alert('Có lỗi xảy ra khi cập nhật!');
    } finally {
      setIsSaving(false);
    }
  };

  const handleBorrowReturn = async (amount: number) => {
      if (!scannedBook || !scannedBook.id || editForm.availableCopies === undefined) return;
      const newCopies = editForm.availableCopies + amount;
      if (newCopies < 0) {
          alert('Sách đã hết, không thể cho mượn thêm!');
          return;
      }
      setEditForm({...editForm, availableCopies: newCopies});
  };

  const handleBorrowAction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!scannedBook || !scannedBook.id || !userCodeInput.trim()) return;

    try {
      setBorrowLoading(true);
      setBorrowError('');
      setBorrowSuccess('');

      const result = await borrowBook(scannedBook.id, userCodeInput.trim().toUpperCase());
      setBorrowSuccess(`Đã cho mượn thành công! Người nhận: ${result.user.displayName || result.user.username}`);
      setUserCodeInput('');

      // Refresh data
      const updatedBooks = await getAllBooks();
      setBooks(updatedBooks || []);
      
      // Update local copy
      const updatedScanned = updatedBooks.find(b => b.id === scannedBook.id);
      if (updatedScanned) {
        setScannedBook(updatedScanned);
        setEditForm({
          title: updatedScanned.title,
          author: updatedScanned.author,
          availableCopies: updatedScanned.availableCopies,
        });
      }

      fetchRecentRecords();
    } catch (err: any) {
      console.error('Lỗi khi mượn sách:', err);
      setBorrowError(err.response?.data?.message || 'Có lỗi xảy ra khi thực hiện mượn sách!');
    } finally {
      setBorrowLoading(false);
    }
  };

  const handleReturnAction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!scannedBook || !scannedBook.id || !userCodeInput.trim()) return;

    try {
      setBorrowLoading(true);
      setBorrowError('');
      setBorrowSuccess('');

      const result = await returnBook(scannedBook.id, userCodeInput.trim().toUpperCase());
      setBorrowSuccess(`Đã nhận trả sách thành công từ: ${result.user.displayName || result.user.username}`);
      setUserCodeInput('');

      // Refresh data
      const updatedBooks = await getAllBooks();
      setBooks(updatedBooks || []);
      
      // Update local copy
      const updatedScanned = updatedBooks.find(b => b.id === scannedBook.id);
      if (updatedScanned) {
        setScannedBook(updatedScanned);
        setEditForm({
          title: updatedScanned.title,
          author: updatedScanned.author,
          availableCopies: updatedScanned.availableCopies,
        });
      }

      fetchRecentRecords();
    } catch (err: any) {
      console.error('Lỗi khi trả sách:', err);
      setBorrowError(err.response?.data?.message || 'Có lỗi xảy ra khi thực hiện trả sách!');
    } finally {
      setBorrowLoading(false);
    }
  };

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="flex flex-col h-full space-y-6 pb-12">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
            <UserCheck className="text-indigo-600" size={32} />
            Nghiệp vụ Thủ thư
          </h1>
          <p className="text-gray-500 mt-2">Mượn, Trả và Đồng bộ Nhãn giấy Điện tử (ESL) thời gian thực</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Scanner Section */}
        <div className="lg:col-span-1 space-y-6">
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
            <div className="flex flex-col items-center justify-center space-y-4 mb-6">
              <div className="w-16 h-16 bg-indigo-50 rounded-full flex items-center justify-center text-indigo-500">
                <Barcode size={32} />
              </div>
              <h2 className="text-lg font-semibold text-gray-700">Quét mã ISBN</h2>
            </div>
            
            <form onSubmit={handleScan} className="flex gap-2">
              <div className="relative flex-1">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Search className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  ref={inputRef}
                  type="text"
                  value={isbnInput}
                  onChange={(e) => setIsbnInput(e.target.value)}
                  placeholder="Ví dụ: ISBN-1001"
                  className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-xl leading-5 bg-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 sm:text-sm font-mono transition-all"
                  autoFocus
                />
              </div>
              <button
                type="submit"
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-xl shadow-sm text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-all"
              >
                Tìm
              </button>
            </form>
            
            {searchError && (
              <motion.p 
                initial={{ opacity: 0, y: -10 }} 
                animate={{ opacity: 1, y: 0 }} 
                className="text-red-500 text-sm mt-3 text-center bg-red-50 py-2 rounded-lg"
              >
                {searchError}
              </motion.p>
            )}
            
            {loading && (
              <p className="text-gray-500 text-sm mt-3 text-center">Đang tải dữ liệu thư viện...</p>
            )}
          </div>
          
          <div className="bg-indigo-50 rounded-2xl p-6 text-sm text-indigo-800 border border-indigo-100">
            <h3 className="font-semibold mb-2 text-indigo-900">Hướng dẫn Nghiệp vụ</h3>
            <ul className="space-y-2 list-disc pl-4 text-indigo-700/80">
              <li>Dùng máy quét mã vạch để quét mã ISBN cuốn sách.</li>
              <li>Chọn tab <strong>Cho Mượn</strong> hoặc <strong>Nhận Trả</strong>.</li>
              <li>Nhập <strong>Mã người dùng (User Code)</strong> của bạn đọc để lưu giao dịch.</li>
              <li>Hệ thống tự động trừ/cộng kho và đồng bộ ngay lên Nhãn giá ESL tại quầy!</li>
            </ul>
          </div>
        </div>

        {/* Book Details, Transaction Flow & Edit */}
        <div className="lg:col-span-2">
          {scannedBook ? (
            <motion.div 
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden"
            >
              <div className="bg-gradient-to-r from-indigo-600 to-purple-600 px-6 py-4 flex items-center justify-between">
                <h3 className="text-white font-medium flex items-center gap-2 text-lg">
                  <BookOpen className="w-5 h-5" />
                  Mã ISBN: <span className="font-bold font-mono tracking-wider">{scannedBook.isbn}</span>
                </h3>
                <span className={`px-3 py-1 text-xs font-semibold rounded-full ${scannedBook.availableCopies > 0 ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                  {scannedBook.availableCopies > 0 ? 'Có sẵn để mượn' : 'Đã mượn hết'}
                </span>
              </div>
              
              <div className="p-6 flex flex-col md:flex-row gap-6">
                {/* Book Cover */}
                <div className="w-full md:w-1/3 bg-gray-100 rounded-xl overflow-hidden shadow-sm flex-shrink-0 border border-gray-200">
                    {scannedBook.coverImageUrl ? (
                        <img src={scannedBook.coverImageUrl} alt="Cover" className="w-full h-full object-cover min-h-[250px]" />
                    ) : (
                        <div className="w-full h-full min-h-[250px] flex items-center justify-center text-gray-400 flex-col gap-2">
                            <BookOpen size={48} />
                            <span>No Cover</span>
                        </div>
                    )}
                </div>

                <div className="flex-1 flex flex-col justify-start">
                  {/* Tab Headers */}
                  <div className="flex border-b border-gray-100 mb-6 bg-gray-50/70 p-1 rounded-xl">
                    <button
                      type="button"
                      onClick={() => { setActiveTab('borrow'); setBorrowError(''); setBorrowSuccess(''); }}
                      className={`flex-1 flex items-center justify-center gap-2 py-2 text-sm font-semibold rounded-lg transition-all ${
                        activeTab === 'borrow'
                          ? 'bg-white text-indigo-600 shadow-sm border border-gray-100'
                          : 'text-gray-500 hover:text-gray-800'
                      }`}
                    >
                      <ArrowRightLeft size={16} />
                      Cho Mượn
                    </button>
                    <button
                      type="button"
                      onClick={() => { setActiveTab('return'); setBorrowError(''); setBorrowSuccess(''); }}
                      className={`flex-1 flex items-center justify-center gap-2 py-2 text-sm font-semibold rounded-lg transition-all ${
                        activeTab === 'return'
                          ? 'bg-white text-indigo-600 shadow-sm border border-gray-100'
                          : 'text-gray-500 hover:text-gray-800'
                      }`}
                    >
                      <RotateCcw size={16} />
                      Nhận Trả
                    </button>
                    <button
                      type="button"
                      onClick={() => { setActiveTab('edit'); setBorrowError(''); setBorrowSuccess(''); }}
                      className={`flex-1 flex items-center justify-center gap-2 py-2 text-sm font-semibold rounded-lg transition-all ${
                        activeTab === 'edit'
                          ? 'bg-white text-indigo-600 shadow-sm border border-gray-100'
                          : 'text-gray-500 hover:text-gray-800'
                      }`}
                    >
                      <Save size={16} />
                      Sửa Sách
                    </button>
                  </div>

                  {/* Tab Body */}
                  <div className="space-y-4">
                    {activeTab === 'borrow' && (
                      <form onSubmit={handleBorrowAction} className="space-y-4">
                        <div className="bg-indigo-50/50 rounded-xl p-4 border border-indigo-100/50 flex flex-col gap-1.5">
                          <span className="text-xs font-semibold text-indigo-700 uppercase tracking-wider">Thông tin sách chọn mượn</span>
                          <span className="text-base font-bold text-gray-800">{scannedBook.title}</span>
                          <span className="text-sm text-gray-500">Tác giả: {scannedBook.author}</span>
                          <div className="flex items-center gap-2 mt-1">
                            <span className="text-xs text-gray-400">Số bản khả dụng:</span>
                            <span className={`px-2 py-0.5 rounded text-xs font-bold ${scannedBook.availableCopies > 0 ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                              {scannedBook.availableCopies} cuốn
                            </span>
                          </div>
                        </div>

                        <div>
                          <label className="block text-sm font-semibold text-gray-700 mb-1 flex items-center gap-1">
                            <UserCheck className="w-4 h-4 text-indigo-500" />
                            Mã số người dùng mượn sách (User Code)
                          </label>
                          <input
                            type="text"
                            value={userCodeInput}
                            onChange={(e) => setUserCodeInput(e.target.value)}
                            placeholder="Ví dụ: LIB12345"
                            required
                            disabled={scannedBook.availableCopies <= 0}
                            className="w-full border-gray-300 rounded-xl shadow-sm focus:border-indigo-500 focus:ring-indigo-500 p-3 bg-gray-50 text-gray-800 font-mono font-bold tracking-wider"
                          />
                        </div>

                        {borrowError && (
                          <div className="p-3 bg-red-50 text-red-700 rounded-xl text-sm flex items-center gap-2 border border-red-100">
                            <AlertTriangle size={16} className="flex-shrink-0" />
                            <span>{borrowError}</span>
                          </div>
                        )}

                        {borrowSuccess && (
                          <div className="p-3 bg-green-50 text-green-700 rounded-xl text-sm flex items-center gap-2 border border-green-100">
                            <CheckCircle size={16} className="flex-shrink-0" />
                            <span>{borrowSuccess}</span>
                          </div>
                        )}

                        <button
                          type="submit"
                          disabled={borrowLoading || scannedBook.availableCopies <= 0}
                          className="w-full inline-flex items-center justify-center gap-2 px-8 py-3 border border-transparent text-base font-semibold rounded-xl shadow-sm text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 transition-all hover:shadow-lg"
                        >
                          {borrowLoading ? 'Đang thực hiện...' : 'Xác Nhận Cho Mượn Sách'}
                        </button>
                      </form>
                    )}

                    {activeTab === 'return' && (
                      <form onSubmit={handleReturnAction} className="space-y-4">
                        <div className="bg-indigo-50/50 rounded-xl p-4 border border-indigo-100/50 flex flex-col gap-1.5">
                          <span className="text-xs font-semibold text-indigo-700 uppercase tracking-wider">Thông tin sách nhận trả</span>
                          <span className="text-base font-bold text-gray-800">{scannedBook.title}</span>
                          <span className="text-sm text-gray-500">Tác giả: {scannedBook.author}</span>
                          <div className="flex items-center gap-2 mt-1">
                            <span className="text-xs text-gray-400">Số bản hiện tại trong kho:</span>
                            <span className="px-2 py-0.5 rounded text-xs font-bold bg-gray-100 text-gray-700">
                              {scannedBook.availableCopies} cuốn
                            </span>
                          </div>
                        </div>

                        <div>
                          <label className="block text-sm font-semibold text-gray-700 mb-1 flex items-center gap-1">
                            <UserCheck className="w-4 h-4 text-indigo-500" />
                            Mã số người dùng trả sách (User Code)
                          </label>
                          <input
                            type="text"
                            value={userCodeInput}
                            onChange={(e) => setUserCodeInput(e.target.value)}
                            placeholder="Ví dụ: LIB12345"
                            required
                            className="w-full border-gray-300 rounded-xl shadow-sm focus:border-indigo-500 focus:ring-indigo-500 p-3 bg-gray-50 text-gray-800 font-mono font-bold tracking-wider"
                          />
                        </div>

                        {borrowError && (
                          <div className="p-3 bg-red-50 text-red-700 rounded-xl text-sm flex items-center gap-2 border border-red-100">
                            <AlertTriangle size={16} className="flex-shrink-0" />
                            <span>{borrowError}</span>
                          </div>
                        )}

                        {borrowSuccess && (
                          <div className="p-3 bg-green-50 text-green-700 rounded-xl text-sm flex items-center gap-2 border border-green-100">
                            <CheckCircle size={16} className="flex-shrink-0" />
                            <span>{borrowSuccess}</span>
                          </div>
                        )}

                        <button
                          type="submit"
                          disabled={borrowLoading}
                          className="w-full inline-flex items-center justify-center gap-2 px-8 py-3 border border-transparent text-base font-semibold rounded-xl shadow-sm text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 transition-all hover:shadow-lg"
                        >
                          {borrowLoading ? 'Đang thực hiện...' : 'Xác Nhận Nhận Trả Sách'}
                        </button>
                      </form>
                    )}

                    {activeTab === 'edit' && (
                      <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                            <Tag className="w-4 h-4 text-gray-400" />
                            Tên sách
                            </label>
                            <input
                            type="text"
                            value={editForm.title}
                            onChange={(e) => setEditForm({...editForm, title: e.target.value})}
                            className="w-full border-gray-300 rounded-xl shadow-sm focus:border-indigo-500 focus:ring-indigo-500 p-3 bg-gray-50 text-gray-800 text-lg font-bold"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                            <UserCheck className="w-4 h-4 text-gray-400" />
                            Tác giả
                            </label>
                            <input
                            type="text"
                            value={editForm.author}
                            onChange={(e) => setEditForm({...editForm, author: e.target.value})}
                            className="w-full border-gray-300 rounded-xl shadow-sm focus:border-indigo-500 focus:ring-indigo-500 p-3 bg-gray-50 text-gray-800 text-lg"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                            <Hash className="w-4 h-4 text-gray-400" />
                            Số lượng bản sao đang rảnh
                            </label>
                            <div className="flex items-center gap-3">
                                <input
                                type="number"
                                min="0"
                                value={editForm.availableCopies}
                                onChange={(e) => setEditForm({...editForm, availableCopies: Number(e.target.value)})}
                                className={`w-24 border-gray-300 rounded-xl shadow-sm text-center p-3 text-xl font-bold ${Number(editForm.availableCopies) === 0 ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700'}`}
                                />
                                <button onClick={() => handleBorrowReturn(-1)} type="button" className="bg-red-100 hover:bg-red-200 text-red-700 px-4 py-3 rounded-xl font-medium transition-colors">- Cho mượn</button>
                                <button onClick={() => handleBorrowReturn(1)} type="button" className="bg-green-100 hover:bg-green-200 text-green-700 px-4 py-3 rounded-xl font-medium transition-colors">+ Nhận trả sách</button>
                            </div>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Bottom Footer Action */}
              <div className="p-4 bg-gray-50 border-t border-gray-100 flex items-center justify-between rounded-b-2xl">
                <button
                type="button"
                onClick={() => {
                    setScannedBook(null);
                    setBorrowError('');
                    setBorrowSuccess('');
                    if (inputRef.current) inputRef.current.focus();
                }}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-xl hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors"
                >
                Hủy / Quét mã khác
                </button>
                
                {activeTab === 'edit' && (
                  <div className="flex items-center gap-3">
                  <AnimatePresence>
                      {saveSuccess && (
                      <motion.span 
                          initial={{ opacity: 0, x: 20 }}
                          animate={{ opacity: 1, x: 0 }}
                          exit={{ opacity: 0 }}
                          className="text-green-600 flex items-center gap-1 text-sm font-medium"
                      >
                          <CheckCircle className="w-4 h-4" /> Đã cập nhật
                      </motion.span>
                      )}
                  </AnimatePresence>
                  
                  <button
                      type="button"
                      onClick={handleSave}
                      disabled={isSaving}
                      className="inline-flex items-center gap-2 px-8 py-3 border border-transparent text-sm font-medium rounded-xl shadow-sm text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-70 transition-all hover:shadow-lg"
                  >
                      {isSaving ? (
                      <>Đang lưu...</>
                      ) : (
                      <>
                          <Save className="w-5 h-5" />
                          Lưu Thay Đổi & Đồng Bộ Nhãn ESL
                      </>
                      )}
                  </button>
                  </div>
                )}
              </div>
            </motion.div>
          ) : (
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12 flex flex-col items-center justify-center h-full min-h-[400px] text-gray-400">
              <div className="w-24 h-24 bg-gray-50 rounded-full flex items-center justify-center mb-4 text-gray-300">
                <BookOpen size={48} />
              </div>
              <h3 className="text-xl font-medium text-gray-500">Chưa có sách nào được quét</h3>
              <p className="text-gray-400 mt-2 text-center max-w-sm">
                Hãy tiến hành nhập hoặc quét mã vạch ISBN phía sau cuốn sách để bắt đầu nghiệp vụ Mượn / Trả.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Transaction History Section */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mt-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-bold text-gray-800 flex items-center gap-2">
              <Clock className="text-indigo-600" size={24} />
              Lịch sử Mượn / Trả Gần Đây
            </h2>
            <p className="text-gray-500 text-sm mt-1">Danh sách các lượt mượn trả sách vừa được thực hiện trong hệ thống</p>
          </div>
          <button 
            onClick={fetchRecentRecords}
            className="p-2 text-indigo-600 hover:bg-indigo-50 rounded-xl transition-all"
            title="Làm mới lịch sử"
          >
            <RotateCcw size={20} />
          </button>
        </div>

        {recordsLoading ? (
          <div className="text-center py-8 text-gray-500">Đang tải lịch sử giao dịch...</div>
        ) : recentRecords.length === 0 ? (
          <div className="text-center py-12 text-gray-400 border border-dashed border-gray-200 rounded-xl">
            Chưa có giao dịch mượn trả nào được lưu trữ.
          </div>
        ) : (
          <div className="overflow-x-auto rounded-xl border border-gray-100">
            <table className="min-w-full divide-y divide-gray-200 text-left">
              <thead className="bg-gray-50 text-gray-500 text-xs font-semibold uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-4">Tên Sách</th>
                  <th className="px-6 py-4">Người Mượn</th>
                  <th className="px-6 py-4">Mã User</th>
                  <th className="px-6 py-4">Ngày Mượn</th>
                  <th className="px-6 py-4">Hạn Trả</th>
                  <th className="px-6 py-4">Ngày Trả</th>
                  <th className="px-6 py-4 text-center">Trạng Thái</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200 text-sm text-gray-700">
                {recentRecords.slice(0, 10).map((record) => (
                  <tr key={record.id} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-6 py-4 font-semibold text-gray-900">{record.book.title}</td>
                    <td className="px-6 py-4">{record.user.displayName || record.user.username}</td>
                    <td className="px-6 py-4 font-mono text-gray-500">{record.user.userCode}</td>
                    <td className="px-6 py-4 flex items-center gap-1.5 text-gray-500">
                      <Calendar size={14} />
                      {formatDate(record.borrowDate)}
                    </td>
                    <td className="px-6 py-4 text-gray-500">{formatDate(record.dueDate)}</td>
                    <td className="px-6 py-4">
                      {record.returnDate ? (
                        <span className="flex items-center gap-1.5 text-green-600">
                          <CheckCircle size={14} />
                          {formatDate(record.returnDate)}
                        </span>
                      ) : (
                        <span className="text-gray-400 font-italic">Chưa trả</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-semibold ${
                        record.status === 'RETURNED' 
                          ? 'bg-green-100 text-green-800' 
                          : record.status === 'OVERDUE' 
                          ? 'bg-red-100 text-red-800' 
                          : 'bg-yellow-100 text-yellow-800'
                      }`}>
                        {record.status === 'RETURNED' ? 'Đã trả' : record.status === 'OVERDUE' ? 'Quá hạn' : 'Đang mượn'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default Librarian;
