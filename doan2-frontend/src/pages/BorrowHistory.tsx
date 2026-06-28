import React, { useEffect, useState } from 'react';
import { motion, Variants } from 'framer-motion';
import { BookOpen, CheckCircle, AlertTriangle, Clock, Calendar, HelpCircle } from 'lucide-react';
import { getHistoryByUser } from '../api/borrow';
import { useAuth } from '../hooks/useAuth';
import { BorrowRecord } from '../types';

export const BorrowHistory: React.FC = () => {
  const { user } = useAuth();
  const [records, setRecords] = useState<BorrowRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchHistory = async () => {
    if (!user?.userCode) {
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const data = await getHistoryByUser(user.userCode);
      setRecords(data || []);
    } catch (err: any) {
      console.error('Error fetching borrow history:', err);
      setError(err.response?.data?.message || 'Không thể tải lịch sử mượn sách. Vui lòng thử lại sau.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, [user?.userCode]);

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getStatusBadge = (status: string, dueDateStr: string) => {
    const isOverdue = status === 'BORROWED' && new Date(dueDateStr) < new Date();
    
    if (isOverdue || status === 'OVERDUE') {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
          <AlertTriangle className="w-3.5 h-3.5" /> Quá hạn
        </span>
      );
    }
    
    if (status === 'RETURNED') {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
          <CheckCircle className="w-3.5 h-3.5" /> Đã trả
        </span>
      );
    }

    return (
      <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
        <Clock className="w-3.5 h-3.5 animate-pulse" /> Đang mượn
      </span>
    );
  };

  // Stats computation
  const activeLoans = records.filter(r => r.status === 'BORROWED').length;
  const returnedLoans = records.filter(r => r.status === 'RETURNED').length;
  const overdueLoans = records.filter(r => {
    return r.status === 'OVERDUE' || (r.status === 'BORROWED' && new Date(r.dueDate) < new Date());
  }).length;

  const containerVariants: Variants = {
    hidden: { opacity: 0 },
    show: {
      opacity: 1,
      transition: { staggerChildren: 0.1 }
    }
  };

  const itemVariants: Variants = {
    hidden: { opacity: 0, y: 20 },
    show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 100 } }
  };

  return (
    <div className="p-6 space-y-8">
      {/* Title block */}
      <div>
        <h1 className="text-3xl font-extrabold text-gray-800 tracking-tight">Lịch sử Mượn trả Sách</h1>
        <p className="text-gray-500 mt-1">Theo dõi các giao dịch mượn, trả và thời hạn sách trong thư viện thông minh của cậu.</p>
      </div>

      {!user?.userCode ? (
        <div className="bg-amber-50 border border-amber-200 rounded-2xl p-6 text-center text-amber-800">
          <HelpCircle className="w-12 h-12 text-amber-500 mx-auto mb-3" />
          <h3 className="font-bold text-lg">Mã số tài khoản chưa được thiết lập</h3>
          <p className="text-sm mt-1 max-w-md mx-auto">
            Hệ thống chưa tìm thấy Mã số độc giả cho tài khoản này. Vui lòng liên hệ Thủ thư/Quản trị viên để được cấp mã và sử dụng dịch vụ mượn trả.
          </p>
        </div>
      ) : loading ? (
        <div className="flex flex-col items-center justify-center py-20">
          <div className="w-12 h-12 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-gray-500 mt-4 font-medium">Đang tải lịch sử mượn sách...</span>
        </div>
      ) : error ? (
        <div className="bg-rose-50 border border-rose-200 text-rose-800 p-6 rounded-2xl text-center">
          <AlertTriangle className="w-12 h-12 text-rose-500 mx-auto mb-3" />
          <h3 className="font-bold text-lg">Đã xảy ra lỗi</h3>
          <p className="text-sm mt-1">{error}</p>
          <button 
            onClick={fetchHistory} 
            className="mt-4 px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl transition-colors shadow-md"
          >
            Thử lại
          </button>
        </div>
      ) : (
        <>
          {/* Stats Cards */}
          <motion.div 
            variants={containerVariants}
            initial="hidden"
            animate="show"
            className="grid grid-cols-1 md:grid-cols-3 gap-6"
          >
            {/* Active loans card */}
            <motion.div 
              variants={itemVariants}
              className="bg-white rounded-2xl border border-slate-100 p-6 shadow-sm flex items-center gap-5 relative overflow-hidden"
            >
              <div className="absolute top-0 right-0 w-24 h-24 bg-indigo-50 rounded-bl-full -z-10 opacity-60"></div>
              <div className="w-12 h-12 bg-indigo-100/70 text-indigo-600 rounded-xl flex items-center justify-center shadow-inner">
                <BookOpen className="w-6 h-6" />
              </div>
              <div>
                <p className="text-gray-400 text-xs font-semibold uppercase tracking-wider">Đang mượn</p>
                <p className="text-3xl font-extrabold text-slate-800 mt-1">{activeLoans}</p>
                <p className="text-indigo-600 text-xs font-medium mt-1">Quyển sách đang giữ</p>
              </div>
            </motion.div>

            {/* Returned card */}
            <motion.div 
              variants={itemVariants}
              className="bg-white rounded-2xl border border-slate-100 p-6 shadow-sm flex items-center gap-5 relative overflow-hidden"
            >
              <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-50 rounded-bl-full -z-10 opacity-60"></div>
              <div className="w-12 h-12 bg-emerald-100/70 text-emerald-600 rounded-xl flex items-center justify-center shadow-inner">
                <CheckCircle className="w-6 h-6" />
              </div>
              <div>
                <p className="text-gray-400 text-xs font-semibold uppercase tracking-wider">Đã trả</p>
                <p className="text-3xl font-extrabold text-slate-800 mt-1">{returnedLoans}</p>
                <p className="text-emerald-600 text-xs font-medium mt-1">Trả sách đúng hạn</p>
              </div>
            </motion.div>

            {/* Overdue card */}
            <motion.div 
              variants={itemVariants}
              className="bg-white rounded-2xl border border-slate-100 p-6 shadow-sm flex items-center gap-5 relative overflow-hidden"
            >
              <div className="absolute top-0 right-0 w-24 h-24 bg-rose-50 rounded-bl-full -z-10 opacity-60"></div>
              <div className="w-12 h-12 bg-rose-100/70 text-rose-600 rounded-xl flex items-center justify-center shadow-inner">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div>
                <p className="text-gray-400 text-xs font-semibold uppercase tracking-wider">Quá hạn</p>
                <p className="text-3xl font-extrabold text-slate-800 mt-1">{overdueLoans}</p>
                <p className="text-rose-600 text-xs font-medium mt-1">Cần trả gấp</p>
              </div>
            </motion.div>
          </motion.div>

          {/* History List */}
          <motion.div 
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="bg-white rounded-2xl border border-slate-100 overflow-hidden shadow-sm"
          >
            <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
              <h2 className="font-bold text-gray-800 text-lg">Danh sách giao dịch mượn sách</h2>
              <span className="text-slate-500 text-sm font-medium">Mã độc giả: <strong className="text-indigo-600 font-bold">{user.userCode}</strong></span>
            </div>

            {records.length === 0 ? (
              <div className="text-center py-20 px-4">
                <div className="text-5xl mb-4">📚</div>
                <h3 className="font-bold text-gray-700 text-lg">Lịch sử trống</h3>
                <p className="text-gray-400 text-sm max-w-sm mx-auto mt-1">
                  Cậu chưa thực hiện giao dịch mượn sách nào trong thư viện cả. Khám phá các đầu sách và bắt đầu đọc ngay nhé!
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-slate-100 text-slate-400 text-xs font-bold uppercase tracking-wider bg-slate-50/20">
                      <th className="py-4 px-6">Thông tin sách</th>
                      <th className="py-4 px-6">Ngày mượn</th>
                      <th className="py-4 px-6">Hạn trả</th>
                      <th className="py-4 px-6">Ngày trả</th>
                      <th className="py-4 px-6 text-center">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    {records.map((record) => {
                      return (
                        <tr 
                          key={record.id} 
                          className="border-b border-slate-100 hover:bg-slate-50/40 transition-colors"
                        >
                          <td className="py-4 px-6">
                            <div className="flex items-center gap-3.5">
                              <div className="w-10 h-14 bg-slate-100 rounded-lg flex-shrink-0 flex items-center justify-center text-xl overflow-hidden shadow-inner border border-slate-100">
                                {record.book.coverImageUrl ? (
                                  <img src={record.book.coverImageUrl} className="w-full h-full object-cover" alt={record.book.title} />
                                ) : (
                                  '📖'
                                )}
                              </div>
                              <div>
                                <h4 className="font-bold text-slate-800 text-sm line-clamp-1">{record.book.title}</h4>
                                <p className="text-slate-400 text-xs mt-0.5">{record.book.author}</p>
                                <span className="inline-block text-[10px] font-mono bg-slate-100 text-slate-500 rounded px-1.5 py-0.5 mt-1">
                                  ISBN: {record.book.isbn}
                                </span>
                              </div>
                            </div>
                          </td>
                          <td className="py-4 px-6 text-sm text-slate-600 font-medium">
                            <div className="flex items-center gap-2">
                              <Calendar className="w-4 h-4 text-slate-400" />
                              {formatDate(record.borrowDate)}
                            </div>
                          </td>
                          <td className="py-4 px-6 text-sm text-slate-600 font-medium">
                            <div className="flex items-center gap-2">
                              <Calendar className="w-4 h-4 text-indigo-400" />
                              {formatDate(record.dueDate)}
                            </div>
                          </td>
                          <td className="py-4 px-6 text-sm text-slate-600 font-medium">
                            {record.returnDate ? (
                              <div className="flex items-center gap-2 text-emerald-600">
                                <Calendar className="w-4 h-4" />
                                {formatDate(record.returnDate)}
                              </div>
                            ) : (
                              <span className="text-slate-300 font-normal italic">Chưa trả</span>
                            )}
                          </td>
                          <td className="py-4 px-6 text-center">
                            {getStatusBadge(record.status, record.dueDate)}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </motion.div>
        </>
      )}
    </div>
  );
};

export default BorrowHistory;
