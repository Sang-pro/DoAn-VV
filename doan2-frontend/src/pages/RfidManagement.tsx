import React, { useEffect, useState, useCallback } from 'react';
import { getAllRfidTags, registerRfidTag, deleteRfidTag } from '../api/rfid';
import { getAllBooks } from '../api/book';
import { RfidTag, Book } from '../types';
import { Bookmark, Trash2, Plus, Search, Tag, X, Calendar, MapPin, CheckCircle2, Activity } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useWebSocket } from '../hooks/useWebSocket';

interface RealtimeScanEntry {
    epc: string;
    location: string;
    timestamp: string;
    isRegistered: boolean;
    bookTitle?: string;
}

interface InventoryBookItem {
    title: string;
    isbn: string;
    author: string;
    count: number;
    epcs: string[];
}

const RfidManagement: React.FC = () => {
    const [tags, setTags] = useState<RfidTag[]>([]);
    const [books, setBooks] = useState<Book[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const [recentScans, setRecentScans] = useState<RealtimeScanEntry[]>([]);
    
    // Sidebar Tabs & Inventory Mode States
    const [sidebarTab, setSidebarTab] = useState<'scans' | 'inventory'>('scans');
    const [isCounting, setIsCounting] = useState(false);
    const [inventory, setInventory] = useState<Record<number, InventoryBookItem>>({});

    // Modal & Form State
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [epc, setEpc] = useState('');
    const [selectedBookId, setSelectedBookId] = useState<number | ''>('');
    const [submitting, setSubmitting] = useState(false);
    const [formError, setFormError] = useState<string | null>(null);

    const fetchData = useCallback(async () => {
        setLoading(true);
        try {
            const [tagsData, booksData] = await Promise.all([getAllRfidTags(), getAllBooks()]);
            setTags(tagsData || []);
            setBooks(booksData || []);
        } catch (err) {
            console.error("Error fetching RFID or Book data:", err);
        } finally {
            setLoading(false);
        }
    }, []);

    const { subscribe, isConnected, errorMsg, currentUrl } = useWebSocket();

    const handleRealtimeScan = useCallback((payload: any) => {
        try {
            const data = typeof payload === 'string' ? JSON.parse(payload) : payload;
            const scannedEpc = data.epc;
            const scannedLocation = data.location || data.deviceId || 'UNKNOWN';

            if (!scannedEpc) return;

            // 1. Auto-fill the EPC input in the registration modal if it is open
            if (isModalOpen) {
                setEpc(scannedEpc);
            }

            // 2. Add to recent scans and update tag in list in real time
            setTags(prevTags => {
                const matchedTag = prevTags.find(tag => tag.epc.toLowerCase() === scannedEpc.toLowerCase());
                
                // If counting mode is active, record in the session inventory
                if (isCounting) {
                    setInventory(prevInv => {
                        const bookId = matchedTag?.book?.id || -1; // -1 for unregistered tags
                        const existing = prevInv[bookId];
                        const epcs = existing ? existing.epcs : [];
                        
                        // Prevent duplicate counts of the same tag in this session
                        if (epcs.some(e => e.toLowerCase() === scannedEpc.toLowerCase())) {
                            return prevInv;
                        }
                        
                        const title = matchedTag?.book?.title || "Mã RFID chưa gán sách";
                        const isbn = matchedTag?.book?.isbn || "N/A";
                        const author = matchedTag?.book?.author || "N/A";
                        
                        return {
                            ...prevInv,
                            [bookId]: {
                                title,
                                isbn,
                                author,
                                count: epcs.length + 1,
                                epcs: [...epcs, scannedEpc]
                            }
                        };
                    });
                }

                setRecentScans(prevScans => {
                    const now = new Date().getTime();
                    // Prevent duplicate scans within 2 seconds
                    if (prevScans.some(s => s.epc.toLowerCase() === scannedEpc.toLowerCase() && (now - new Date(s.timestamp).getTime()) < 2000)) {
                        return prevScans;
                    }
                    const newScan: RealtimeScanEntry = {
                        epc: scannedEpc,
                        location: scannedLocation,
                        timestamp: new Date().toISOString(),
                        isRegistered: !!matchedTag,
                        bookTitle: matchedTag?.book?.title
                    };
                    return [newScan, ...prevScans].slice(0, 5);
                });

                return prevTags.map(tag => {
                    if (tag.epc.toLowerCase() === scannedEpc.toLowerCase()) {
                        return {
                            ...tag,
                            currentLocation: scannedLocation,
                            lastScannedAt: new Date().toISOString()
                        };
                    }
                    return tag;
                });
            });
        } catch (err) {
            console.error("Error processing real-time RFID scan:", err);
        }
    }, [isModalOpen, isCounting]);

    useEffect(() => {
        if (!isConnected) return;

        const unsubscribeScan = subscribe('warehouse/rfid/scan', (message) => {
            handleRealtimeScan(message.message);
        });

        const unsubscribeGate = subscribe('warehouse/rfid/gate', (message) => {
            handleRealtimeScan(message.message);
        });

        return () => {
            unsubscribeScan();
            unsubscribeGate();
        };
    }, [isConnected, subscribe, handleRealtimeScan]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    // Keep recent scans in sync with tags updates (e.g. when a new tag is registered)
    useEffect(() => {
        setRecentScans(prevScans => 
            prevScans.map(scan => {
                const matchedTag = tags.find(tag => tag.epc.toLowerCase() === scan.epc.toLowerCase());
                if (matchedTag && !scan.isRegistered) {
                    return {
                        ...scan,
                        isRegistered: true,
                        bookTitle: matchedTag.book?.title
                    };
                }
                return scan;
            })
        );
    }, [tags]);

    const handleOpenModal = () => {
        setEpc('');
        setSelectedBookId('');
        setFormError(null);
        setIsModalOpen(true);
    };

    const handleDelete = async (id: number) => {
        if (window.confirm("Bạn có chắc chắn muốn xóa/gỡ mã dán RFID này?")) {
            try {
                await deleteRfidTag(id);
                fetchData();
            } catch (err) {
                console.error("Error deleting RFID tag:", err);
                alert("Không thể xóa thẻ RFID này.");
            }
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedBookId) {
            setFormError("Vui lòng chọn sách để gắn mã.");
            return;
        }
        if (!epc.trim()) {
            setFormError("Vui lòng nhập mã EPC.");
            return;
        }

        setSubmitting(true);
        setFormError(null);
        try {
            await registerRfidTag(epc.trim(), Number(selectedBookId));
            setIsModalOpen(false);
            fetchData();
        } catch (err: any) {
            console.error("Error registering RFID tag:", err);
            setFormError(err.response?.data?.message || "Lỗi khi đăng ký thẻ RFID. Có thể mã EPC đã tồn tại.");
        } finally {
            setSubmitting(false);
        }
    };

    const filteredTags = tags.filter(tag => {
        const bookTitle = tag.book?.title?.toLowerCase() || '';
        const bookAuthor = tag.book?.author?.toLowerCase() || '';
        const epcCode = tag.epc.toLowerCase();
        const location = tag.currentLocation?.toLowerCase() || '';
        const query = searchQuery.toLowerCase();
        return bookTitle.includes(query) || bookAuthor.includes(query) || epcCode.includes(query) || location.includes(query);
    });

    return (
        <div className="space-y-6">
            {/* Header section with Stats */}
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                <div>
                    <h1 className="text-3xl font-extrabold text-slate-800 tracking-tight">Quản lý Mã dán RFID</h1>
                    <p className="text-slate-500 mt-1">Đăng ký và quản lý các thẻ RFID dán trên sách phục vụ giám sát cổng an ninh và vị trí.</p>
                </div>
                <motion.button 
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={handleOpenModal} 
                    className="flex items-center gap-2 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 text-white font-semibold px-4 py-2.5 rounded-xl shadow-md shadow-indigo-200 transition-all duration-200"
                >
                    <Plus className="w-5 h-5" />
                    <span>Đăng ký mã mới</span>
                </motion.button>
            </div>

            {/* Quick stats cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
                <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex items-center gap-4">
                    <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                        <Tag className="w-6 h-6" />
                    </div>
                    <div>
                        <div className="text-slate-400 text-xs font-semibold uppercase tracking-wider">Tổng số mã dán</div>
                        <div className="text-2xl font-bold text-slate-800 mt-1">{tags.length} thẻ</div>
                    </div>
                </div>
                <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex items-center gap-4">
                    <div className="w-12 h-12 rounded-xl bg-green-50 text-green-600 flex items-center justify-center">
                        <CheckCircle2 className="w-6 h-6" />
                    </div>
                    <div>
                        <div className="text-slate-400 text-xs font-semibold uppercase tracking-wider">Liên kết đầu sách</div>
                        <div className="text-2xl font-bold text-slate-800 mt-1">
                            {new Set(tags.map(t => t.book?.id).filter(Boolean)).size} đầu sách
                        </div>
                    </div>
                </div>
                <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex items-center gap-4">
                    <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
                        <Bookmark className="w-6 h-6" />
                    </div>
                    <div>
                        <div className="text-slate-400 text-xs font-semibold uppercase tracking-wider">Đầu sách chưa dán mã</div>
                        <div className="text-2xl font-bold text-slate-800 mt-1">
                            {Math.max(0, books.length - new Set(tags.map(t => t.book?.id).filter(Boolean)).size)} đầu sách
                        </div>
                    </div>
                </div>
            </div>

            {/* Search and Filters */}
            <div className="bg-white p-4 rounded-2xl border border-slate-100 shadow-sm flex items-center gap-3">
                <Search className="w-5 h-5 text-slate-400" />
                <input 
                    type="text" 
                    placeholder="Tìm kiếm theo mã EPC, tên sách, tác giả, vị trí..." 
                    value={searchQuery}
                    onChange={e => setSearchQuery(e.target.value)}
                    className="w-full bg-transparent border-none outline-none text-slate-700 placeholder:text-slate-400 text-sm"
                />
            </div>

            {/* Main Content Area */}
            {loading ? (
                <div className="flex justify-center items-center py-20">
                    <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600"></div>
                </div>
            ) : (
                <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                    {/* Left side: Registered RFID Tags Table */}
                    <div className="lg:col-span-3 bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden flex flex-col justify-between">
                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr className="border-b border-slate-100 bg-slate-50/70 text-slate-500 font-semibold text-xs uppercase tracking-wider">
                                        <th className="px-6 py-4">Mã EPC (RFID)</th>
                                        <th className="px-6 py-4">Sách liên kết</th>
                                        <th className="px-6 py-4">Vị trí hiện tại</th>
                                        <th className="px-6 py-4">Lần quét cuối</th>
                                        <th className="px-6 py-4 text-right">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-50 text-sm">
                                    {filteredTags.length === 0 ? (
                                        <tr>
                                            <td colSpan={5} className="px-6 py-10 text-center text-slate-400">
                                                Không tìm thấy mã dán RFID nào phù hợp.
                                            </td>
                                        </tr>
                                    ) : (
                                        filteredTags.map((tag) => (
                                            <tr key={tag.id} className="hover:bg-slate-50/40 transition-colors">
                                                <td className="px-6 py-4 font-mono font-bold text-slate-700 tracking-tight">
                                                    {tag.epc}
                                                </td>
                                                <td className="px-6 py-4">
                                                    {tag.book ? (
                                                        <div>
                                                            <div className="font-semibold text-slate-800">{tag.book.title}</div>
                                                            <div className="text-xs text-slate-400 mt-0.5">Tác giả: {tag.book.author} | ISBN: {tag.book.isbn}</div>
                                                        </div>
                                                    ) : (
                                                        <span className="text-red-500 font-medium">Chưa gán</span>
                                                    )}
                                                </td>
                                                <td className="px-6 py-4">
                                                    <div className="flex items-center gap-1.5 text-slate-600">
                                                        <MapPin className="w-4 h-4 text-slate-400" />
                                                        <span>{tag.currentLocation || "Chưa quét"}</span>
                                                    </div>
                                                </td>
                                                <td className="px-6 py-4 text-slate-500">
                                                    <div className="flex items-center gap-1.5">
                                                        <Calendar className="w-4 h-4 text-slate-400" />
                                                        <span>
                                                            {tag.lastScannedAt 
                                                                ? new Date(tag.lastScannedAt).toLocaleString('vi-VN') 
                                                                : "N/A"
                                                            }
                                                        </span>
                                                    </div>
                                                </td>
                                                <td className="px-6 py-4 text-right">
                                                    <button 
                                                        onClick={() => tag.id && handleDelete(tag.id)} 
                                                        className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all inline-flex items-center justify-center"
                                                        title="Xóa liên kết"
                                                    >
                                                        <Trash2 className="w-4 h-4" />
                                                    </button>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Right side: Real-time Scan Log & Inventory Sidebar */}
                    <div className="lg:col-span-1 flex flex-col gap-4">
                        <div className="bg-white p-5 rounded-2xl border border-slate-100 shadow-sm flex-1 flex flex-col">
                            {/* Tab Switcher */}
                            <div className="flex border-b border-slate-100 pb-3 mb-4 gap-2">
                                <button
                                    onClick={() => setSidebarTab('scans')}
                                    className={`flex-1 text-xs font-bold py-2 rounded-xl transition-all duration-200 text-center ${
                                        sidebarTab === 'scans'
                                            ? 'bg-indigo-50 text-indigo-700 border border-indigo-100/50'
                                            : 'text-slate-500 hover:bg-slate-50'
                                    }`}
                                >
                                    Lịch sử quét
                                </button>
                                <button
                                    onClick={() => setSidebarTab('inventory')}
                                    className={`flex-1 text-xs font-bold py-2 rounded-xl transition-all duration-200 text-center flex items-center justify-center gap-1.5 ${
                                        sidebarTab === 'inventory'
                                            ? 'bg-indigo-50 text-indigo-700 border border-indigo-100/50'
                                            : 'text-slate-500 hover:bg-slate-50'
                                    }`}
                                >
                                    <Activity className="w-3.5 h-3.5" />
                                    Kiểm đếm sách
                                </button>
                            </div>

                            {/* Connection status (Always visible) */}
                            <div className="flex items-center justify-between pb-3 border-b border-slate-50 mb-3">
                                <div className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">Trạng thái kết nối</div>
                                <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold flex items-center gap-1.5 ${
                                    isConnected ? 'bg-emerald-50 text-emerald-600 border border-emerald-100' : 'bg-rose-50 text-rose-600 border border-rose-100'
                                }`}>
                                    <span className={`w-1.5 h-1.5 rounded-full ${isConnected ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500'}`}></span>
                                    {isConnected ? 'Liên kết' : 'Mất kết nối'}
                                </span>
                            </div>

                            {!isConnected && (
                                <div className="text-[11px] text-rose-600 mt-1 mb-3 bg-rose-50/50 border border-rose-100/60 p-2.5 rounded-xl flex flex-col gap-1">
                                    <div className="font-semibold">Chi tiết lỗi kết nối:</div>
                                    <div className="font-mono bg-white/50 p-1.5 rounded border border-rose-100 text-[10px] break-all">{errorMsg || "Không có phản hồi từ socket"}</div>
                                    <div className="text-slate-500 mt-0.5 text-[10px]">Đang thử kết nối URL: <code className="font-mono text-slate-700 bg-slate-100 px-1 py-0.5 rounded break-all">{currentUrl || "N/A"}</code></div>
                                </div>
                            )}

                            {/* Tab 1: Recent Scans Log */}
                            {sidebarTab === 'scans' && (
                                <div className="flex-1 space-y-3 max-h-[450px] overflow-y-auto pr-1">
                                    {recentScans.length === 0 ? (
                                        <div className="text-center py-12 text-slate-400 text-xs flex flex-col items-center justify-center h-full gap-2">
                                            <Activity className="w-8 h-8 text-slate-300 animate-pulse" />
                                            <span>Đang đợi quét thẻ RFID...</span>
                                            <span className="text-[10px] text-slate-400 max-w-[150px] mx-auto">
                                                Quét thẻ bằng đầu đọc hoặc giả lập MQTT để thấy hoạt động.
                                            </span>
                                        </div>
                                    ) : (
                                        <AnimatePresence initial={false}>
                                            {recentScans.map((scan, idx) => (
                                                <motion.div
                                                    key={scan.timestamp + idx}
                                                    initial={{ opacity: 0, y: -10, scale: 0.95 }}
                                                    animate={{ opacity: 1, y: 0, scale: 1 }}
                                                    exit={{ opacity: 0, scale: 0.95 }}
                                                    transition={{ duration: 0.2 }}
                                                    className={`p-3.5 rounded-xl border transition-all text-xs flex flex-col gap-2 relative overflow-hidden group ${
                                                        scan.isRegistered 
                                                            ? 'bg-emerald-50/20 border-emerald-100 text-emerald-950' 
                                                            : 'bg-amber-50/30 border-amber-100 text-amber-950'
                                                    }`}
                                                >
                                                    {/* Card Background Gradient Blob */}
                                                    <div className={`absolute -right-6 -bottom-6 w-16 h-16 rounded-full blur-xl pointer-events-none opacity-20 ${
                                                        scan.isRegistered ? 'bg-emerald-400' : 'bg-amber-400'
                                                    }`} />

                                                    <div className="flex justify-between items-start">
                                                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold tracking-wide ${
                                                            scan.isRegistered 
                                                                ? 'bg-emerald-100/70 text-emerald-800 border border-emerald-200/50' 
                                                                : 'bg-amber-100/70 text-amber-800 border border-amber-200/50'
                                                        }`}>
                                                            {scan.isRegistered ? 'Đã liên kết' : 'Chưa liên kết'}
                                                        </span>
                                                        <span className="text-slate-400 text-[10px]">
                                                            {new Date(scan.timestamp).toLocaleTimeString('vi-VN')}
                                                        </span>
                                                    </div>

                                                    <div className="flex flex-col gap-1">
                                                        <div className="font-mono font-bold text-slate-800 tracking-tight break-all">
                                                            {scan.epc}
                                                        </div>
                                                        <div className="flex items-center gap-1.5 text-slate-500 mt-0.5">
                                                            <MapPin className="w-3.5 h-3.5" />
                                                            <span>Vị trí: {scan.location}</span>
                                                        </div>
                                                    </div>

                                                    {scan.isRegistered ? (
                                                        <div className="bg-emerald-100/30 border border-emerald-100/50 p-2 rounded-lg text-emerald-800 font-medium">
                                                            Sách: {scan.bookTitle}
                                                        </div>
                                                    ) : (
                                                        <div className="flex justify-between items-center bg-amber-100/20 border border-amber-100/40 p-2 rounded-lg mt-0.5">
                                                            <span className="text-amber-700 font-medium">Cần gắn sách để sử dụng</span>
                                                            <button
                                                                onClick={() => {
                                                                    setEpc(scan.epc);
                                                                    setSelectedBookId('');
                                                                    setFormError(null);
                                                                    setIsModalOpen(true);
                                                                }}
                                                                className="flex items-center gap-1 bg-amber-600 hover:bg-amber-700 text-white font-semibold px-2 py-1 rounded-lg shadow-sm transition-all"
                                                            >
                                                                <Plus className="w-3.5 h-3.5" />
                                                                <span>Gắn</span>
                                                            </button>
                                                        </div>
                                                    )}
                                                </motion.div>
                                            ))}
                                        </AnimatePresence>
                                    )}
                                </div>
                            )}

                            {/* Tab 2: Inventory Mode */}
                            {sidebarTab === 'inventory' && (
                                <div className="flex-grow flex flex-col gap-4 overflow-hidden">
                                    {/* Counting Controller */}
                                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-100 flex flex-col gap-3">
                                        <div className="flex items-center justify-between">
                                            <span className="text-xs font-bold text-slate-700">Chế độ đếm sách</span>
                                            <button
                                                onClick={() => setIsCounting(!isCounting)}
                                                className={`text-[11px] font-bold px-3 py-1.5 rounded-lg transition-all ${
                                                    isCounting
                                                        ? 'bg-rose-600 text-white shadow-sm hover:bg-rose-700'
                                                        : 'bg-emerald-600 text-white shadow-sm hover:bg-emerald-700'
                                                }`}
                                            >
                                                {isCounting ? 'Tạm dừng' : 'Bắt đầu đếm'}
                                            </button>
                                        </div>
                                        <div className="flex items-center justify-between text-[11px]">
                                            <span className="text-slate-400">Đã quét: <strong className="text-slate-700">{Object.keys(inventory).length} đầu sách</strong></span>
                                            <button
                                                onClick={() => setInventory({})}
                                                className="text-slate-500 hover:text-red-600 font-semibold"
                                            >
                                                Đặt lại
                                            </button>
                                        </div>
                                    </div>

                                    {/* Scanned Inventory Items List */}
                                    <div className="flex-1 space-y-3 max-h-[350px] overflow-y-auto pr-1">
                                        {Object.keys(inventory).length === 0 ? (
                                            <div className="text-center py-12 text-slate-400 text-xs flex flex-col items-center justify-center h-full gap-2">
                                                <Activity className="w-8 h-8 text-slate-300 animate-pulse" />
                                                <span>Chưa có kết quả kiểm đếm</span>
                                                <span className="text-[10px] text-slate-400 max-w-[150px] mx-auto">
                                                    Bật "Bắt đầu đếm" và quét sách để thống kê số lượng.
                                                </span>
                                            </div>
                                        ) : (
                                            Object.entries(inventory).map(([bookId, item]) => {
                                                const bId = Number(bookId);
                                                const isUnregistered = bId === -1;
                                                
                                                // Tìm sách trong cơ sở dữ liệu để lấy số lượng bản sao khả dụng (availableCopies)
                                                const matchedBook = books.find(b => b.id === bId);
                                                const dbCopies = matchedBook ? matchedBook.availableCopies : 0;
                                                
                                                // Lấy tất cả mã EPC đã đăng ký trong hệ thống cho đầu sách này
                                                const bookTags = tags.filter(t => t.book?.id === bId);
                                                const regEpcs = bookTags.map(t => t.epc);

                                                return (
                                                    <div 
                                                        key={bookId}
                                                        className={`p-3 rounded-xl border border-slate-100 text-xs flex flex-col gap-2 shadow-sm relative overflow-hidden ${
                                                            isUnregistered ? 'bg-amber-50/20 border-amber-100' : 'bg-indigo-50/5 border-indigo-100/30'
                                                        }`}
                                                    >
                                                        <div className="flex justify-between items-start">
                                                            <div className="font-bold text-slate-800 line-clamp-2 pr-2">{item.title}</div>
                                                            <span className="bg-indigo-100 text-indigo-800 text-[10px] font-extrabold px-2.5 py-1 rounded-full shrink-0">
                                                                Đã đếm: {item.count} bản
                                                            </span>
                                                        </div>
                                                        
                                                        {!isUnregistered && (
                                                            <div className="flex justify-between items-center bg-slate-100/50 p-2 rounded-lg text-[10px] text-slate-500 mt-1">
                                                                <span className="truncate pr-1">Tác giả: <strong>{item.author}</strong></span>
                                                                <span className="font-semibold text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded shrink-0">
                                                                    Kho DB: {dbCopies} bản
                                                                </span>
                                                            </div>
                                                        )}

                                                        {/* Hiển thị cảnh báo lệch số lượng */}
                                                        {!isUnregistered && item.count !== dbCopies && (
                                                            <div className={`text-[10px] px-2 py-1 rounded-lg font-medium flex items-center gap-1 ${
                                                                item.count < dbCopies 
                                                                    ? 'bg-amber-50 text-amber-700 border border-amber-100' 
                                                                    : 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                                                            }`}>
                                                                <span>⚠️</span>
                                                                <span>
                                                                    {item.count < dbCopies 
                                                                        ? `Thiếu ${dbCopies - item.count} bản so với hệ thống` 
                                                                        : `Thừa ${item.count - dbCopies} bản so với hệ thống`}
                                                                </span>
                                                            </div>
                                                        )}

                                                        {/* Danh sách chi tiết các mã EPC */}
                                                        <div className="bg-slate-50 p-2 rounded-lg border border-slate-100 mt-1">
                                                            <div className="text-[9px] text-slate-400 uppercase font-semibold">Trạng thái mã EPC của đầu sách:</div>
                                                            <div className="max-h-[90px] overflow-y-auto mt-1 space-y-1 custom-scrollbar">
                                                                {isUnregistered ? (
                                                                    // Thẻ chưa đăng ký
                                                                    item.epcs.map((epcCode, eIdx) => (
                                                                        <div key={eIdx} className="flex justify-between items-center text-[10px] bg-amber-50/50 p-1 rounded font-mono text-amber-800">
                                                                            <span>• {epcCode}</span>
                                                                            <span className="text-[8px] bg-amber-100 px-1 rounded font-sans font-bold">Chưa liên kết</span>
                                                                        </div>
                                                                    ))
                                                                ) : (
                                                                    // Thẻ đã đăng ký (so sánh quét vs chưa quét)
                                                                    regEpcs.map((epcCode, eIdx) => {
                                                                        const isScanned = item.epcs.some(e => e.toLowerCase() === epcCode.toLowerCase());
                                                                        return (
                                                                            <div key={eIdx} className={`flex justify-between items-center text-[10px] p-1 rounded font-mono ${
                                                                                isScanned 
                                                                                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-100/50' 
                                                                                    : 'bg-slate-100 text-slate-400 border border-slate-200/50 line-through decoration-slate-300'
                                                                            }`}>
                                                                                <span className="truncate pr-1">• {epcCode}</span>
                                                                                <span className={`text-[8px] px-1 rounded font-sans font-bold uppercase shrink-0 ${
                                                                                    isScanned ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-500'
                                                                                }`}>
                                                                                    {isScanned ? 'Đã đếm' : 'Chưa quét'}
                                                                                </span>
                                                                            </div>
                                                                        );
                                                                    })
                                                                )}
                                                                
                                                                {/* Mã lạ quét trúng nhưng không có trong danh sách đăng ký của sách */}
                                                                {!isUnregistered && item.epcs.filter(e => !regEpcs.some(re => re.toLowerCase() === e.toLowerCase())).map((epcCode, eIdx) => (
                                                                    <div key={eIdx} className="flex justify-between items-center text-[10px] bg-red-50 text-red-800 p-1 rounded font-mono border border-red-100/50">
                                                                        <span className="truncate pr-1">• {epcCode}</span>
                                                                        <span className="text-[8px] bg-red-100 text-red-800 px-1 rounded font-sans font-bold uppercase shrink-0">Lạ / Nhầm</span>
                                                                    </div>
                                                                ))}
                                                            </div>
                                                        </div>
                                                    </div>
                                                );
                                            })
                                        )}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Register Modal */}
            <AnimatePresence>
                {isModalOpen && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
                        {/* Overlay */}
                        <motion.div 
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            onClick={() => setIsModalOpen(false)}
                            className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm"
                        />
                        
                        {/* Modal Box */}
                        <motion.div 
                            initial={{ opacity: 0, scale: 0.95, y: 10 }}
                            animate={{ opacity: 1, scale: 1, y: 0 }}
                            exit={{ opacity: 0, scale: 0.95, y: 10 }}
                            className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden z-10 border border-slate-100"
                        >
                            <div className="flex justify-between items-center px-6 py-4 border-b border-slate-100">
                                <h3 className="text-lg font-bold text-slate-800">Đăng ký gắn mã RFID</h3>
                                <button 
                                    onClick={() => setIsModalOpen(false)}
                                    className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-50 transition-colors"
                                >
                                    <X className="w-5 h-5" />
                                </button>
                            </div>

                            <form onSubmit={handleSubmit} className="p-6 space-y-4">
                                {formError && (
                                    <div className="p-3 bg-red-50 border border-red-100 text-red-700 text-sm rounded-xl font-medium">
                                        {formError}
                                    </div>
                                )}

                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">Mã EPC (Mã thẻ RFID)</label>
                                    <input 
                                        type="text"
                                        required 
                                        placeholder="Ví dụ: E280691500005028..." 
                                        value={epc} 
                                        onChange={e => setEpc(e.target.value)} 
                                        className="w-full border border-slate-200 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 rounded-xl px-4 py-2.5 outline-none text-sm font-mono tracking-wide"
                                    />
                                    <p className="text-xs text-slate-400 mt-1">Mã EPC của thẻ dán RFID cần liên kết.</p>
                                </div>

                                <div>
                                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">Sách cần gắn mã</label>
                                    <select 
                                        required
                                        value={selectedBookId} 
                                        onChange={e => setSelectedBookId(e.target.value ? Number(e.target.value) : '')}
                                        className="w-full border border-slate-200 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 rounded-xl px-3 py-2.5 outline-none text-sm bg-white"
                                    >
                                        <option value="">-- Chọn sách --</option>
                                        {books.map(b => (
                                            <option key={b.id} value={b.id}>
                                                {b.title} (ISBN: {b.isbn})
                                            </option>
                                        ))}
                                    </select>
                                    <p className="text-xs text-slate-400 mt-1">Chọn sách để liên kết với mã thẻ RFID trên.</p>
                                </div>

                                <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                                    <button 
                                        type="button" 
                                        onClick={() => setIsModalOpen(false)} 
                                        className="px-4 py-2 rounded-xl text-slate-500 hover:bg-slate-50 font-semibold text-sm transition-colors"
                                    >
                                        Hủy
                                    </button>
                                    <button 
                                        type="submit" 
                                        disabled={submitting}
                                        className="px-4 py-2 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-semibold text-sm rounded-xl shadow-md shadow-indigo-100 hover:from-indigo-700 hover:to-purple-700 disabled:opacity-50 transition-all"
                                    >
                                        {submitting ? "Đang xử lý..." : "Lưu thông tin"}
                                    </button>
                                </div>
                            </form>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default RfidManagement;
