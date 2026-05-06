import React, { useState, useEffect, useRef } from 'react';
import { Search, Save, Package, Tag, Hash, ShoppingCart, Barcode, CheckCircle } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { getAllProducts, updateProduct } from '../api/product';
import { Product } from '../types';

export const Cashier: React.FC = () => {
  const [skuInput, setSkuInput] = useState('');
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [scannedProduct, setScannedProduct] = useState<Product | null>(null);
  
  // Form state
  const [editForm, setEditForm] = useState<Partial<Product>>({
    name: '',
    price: 0,
    stockQuantity: 0,
  });
  
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  
  const inputRef = useRef<HTMLInputElement>(null);

  // Fetch all products once so we can search instantly by SKU
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        const data = await getAllProducts();
        setProducts(data || []);
      } catch (err) {
        console.error('Failed to fetch products:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchProducts();
    
    // Auto focus the input when page loads (useful for barcode scanners)
    if (inputRef.current) {
      inputRef.current.focus();
    }
  }, []);

  const handleScan = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!skuInput.trim()) return;

    setSearchError('');
    setSaveSuccess(false);

    const found = products.find(p => p.sku === skuInput.trim() || p.sku === skuInput.trim().toUpperCase());
    
    if (found) {
      setScannedProduct(found);
      setEditForm({
        name: found.name,
        price: found.price,
        stockQuantity: found.stockQuantity,
      });
      // Clear input after successful scan so it's ready for the next one
      setSkuInput('');
    } else {
      setScannedProduct(null);
      setSearchError('Không tìm thấy sản phẩm với mã này!');
    }
  };

  const handleSave = async () => {
    if (!scannedProduct || !scannedProduct.id) return;
    
    try {
      setIsSaving(true);
      setSaveSuccess(false);
      const updated = await updateProduct(scannedProduct.id, {
        ...scannedProduct,
        name: editForm.name,
        price: editForm.price,
        stockQuantity: editForm.stockQuantity,
      });
      
      // Update local product list
      setProducts(products.map(p => p.id === updated.id ? updated : p));
      setScannedProduct(updated);
      setSaveSuccess(true);
      
      setTimeout(() => setSaveSuccess(false), 3000);
      
      // Focus back to scanner input
      if (inputRef.current) {
        inputRef.current.focus();
      }
    } catch (err) {
      console.error('Lỗi khi cập nhật sản phẩm:', err);
      alert('Có lỗi xảy ra khi cập nhật!');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="flex flex-col h-full space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            <ShoppingCart className="text-indigo-600" />
            Màn hình Thu Ngân
          </h1>
          <p className="text-gray-500 mt-1">Quét mã sản phẩm để xem và cập nhật thông tin nhanh</p>
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
              <h2 className="text-lg font-semibold text-gray-700">Quét / Nhập mã SKU</h2>
            </div>
            
            <form onSubmit={handleScan} className="flex gap-2">
              <div className="relative flex-1">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Search className="h-5 w-5 text-gray-400" />
                </div>
                <input
                  ref={inputRef}
                  type="text"
                  value={skuInput}
                  onChange={(e) => setSkuInput(e.target.value)}
                  placeholder="Ví dụ: P001"
                  className="block w-full pl-10 pr-3 py-3 border border-gray-300 rounded-xl leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 sm:text-sm font-mono transition-all"
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
              <p className="text-gray-500 text-sm mt-3 text-center">Đang tải dữ liệu sản phẩm...</p>
            )}
          </div>
          
          <div className="bg-indigo-50 rounded-2xl p-6 text-sm text-indigo-800 border border-indigo-100">
            <h3 className="font-semibold mb-2 text-indigo-900">Hướng dẫn Thu ngân</h3>
            <ul className="space-y-2 list-disc pl-4 text-indigo-700/80">
              <li>Sử dụng máy quét mã vạch để quét trực tiếp. Con trỏ sẽ tự động đặt ở ô nhập.</li>
              <li>Thay đổi tên, giá bán, thẻ kho nếu phát hiện sai sót hoặc cập nhật giá mới.</li>
              <li>Ấn "Cập nhật sản phẩm" để lưu trực tiếp xuống hệ thống và nhãn ESL.</li>
            </ul>
          </div>
        </div>

        {/* Product Details & Edit Section */}
        <div className="lg:col-span-2">
          {scannedProduct ? (
            <motion.div 
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden"
            >
              <div className="bg-gradient-to-r from-indigo-600 to-purple-600 px-6 py-4 flex items-center justify-between">
                <h3 className="text-white font-medium flex items-center gap-2 text-lg">
                  <Package className="w-5 h-5" />
                  Mã SKU: <span className="font-bold font-mono tracking-wider">{scannedProduct.sku}</span>
                </h3>
                <span className={`px-3 py-1 text-xs font-semibold rounded-full ${scannedProduct.stockQuantity > 0 ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                  {scannedProduct.stockQuantity > 0 ? 'Còn hàng' : 'Hết hàng'}
                </span>
              </div>
              
              <div className="p-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                  {/* Name field */}
                  <div className="col-span-1 md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                      <Tag className="w-4 h-4 text-gray-400" />
                      Tên sản phẩm
                    </label>
                    <input
                      type="text"
                      value={editForm.name}
                      onChange={(e) => setEditForm({...editForm, name: e.target.value})}
                      className="w-full border-gray-300 rounded-xl shadow-sm focus:border-indigo-500 focus:ring-indigo-500 p-3 bg-gray-50 text-gray-800 text-lg font-medium"
                    />
                  </div>

                  {/* Price field */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                      <span className="text-gray-400 font-bold">$</span>
                      Giá bán (VND)
                    </label>
                    <div className="relative">
                      <input
                        type="number"
                        min="0"
                        value={editForm.price}
                        onChange={(e) => setEditForm({...editForm, price: Number(e.target.value)})}
                        className="w-full border-gray-300 rounded-xl shadow-sm focus:border-indigo-500 focus:ring-indigo-500 p-3 bg-gray-50 text-gray-800 text-xl font-bold pr-12"
                      />
                      <div className="absolute inset-y-0 right-0 pr-4 flex items-center pointer-events-none">
                        <span className="text-gray-500">đ</span>
                      </div>
                    </div>
                  </div>

                  {/* Stock field */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                      <Hash className="w-4 h-4 text-gray-400" />
                      Số lượng tồn kho
                    </label>
                    <input
                      type="number"
                      min="0"
                      value={editForm.stockQuantity}
                      onChange={(e) => setEditForm({...editForm, stockQuantity: Number(e.target.value)})}
                      className={`w-full border-gray-300 rounded-xl shadow-sm focus:border-indigo-500 focus:ring-indigo-500 p-3 text-lg font-bold ${Number(editForm.stockQuantity) === 0 ? 'bg-red-50 text-red-700' : 'bg-gray-50 text-gray-800'}`}
                    />
                  </div>
                </div>

                <div className="pt-4 border-t border-gray-100 flex items-center justify-between">
                  <button
                    type="button"
                    onClick={() => {
                      setScannedProduct(null);
                      if (inputRef.current) inputRef.current.focus();
                    }}
                    className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-xl hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-colors"
                  >
                    Hủy / Quét mã khác
                  </button>
                  
                  <div className="flex items-center gap-3">
                    <AnimatePresence>
                      {saveSuccess && (
                        <motion.span 
                          initial={{ opacity: 0, x: 20 }}
                          animate={{ opacity: 1, x: 0 }}
                          exit={{ opacity: 0 }}
                          className="text-green-600 flex items-center gap-1 text-sm font-medium"
                        >
                          <CheckCircle className="w-4 h-4" /> Đã lưu thành công
                        </motion.span>
                      )}
                    </AnimatePresence>
                    
                    <button
                      type="button"
                      onClick={handleSave}
                      disabled={isSaving}
                      className="inline-flex items-center gap-2 px-6 py-3 border border-transparent text-sm font-medium rounded-xl shadow-sm text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-70 transition-all hover:shadow-lg hover:-translate-y-0.5"
                    >
                      {isSaving ? (
                        <>Đang lưu...</>
                      ) : (
                        <>
                          <Save className="w-4 h-4" />
                          Cập nhật sản phẩm
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </div>
            </motion.div>
          ) : (
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-12 flex flex-col items-center justify-center h-full min-h-[400px] text-gray-400">
              <div className="w-24 h-24 bg-gray-50 rounded-full flex items-center justify-center mb-4 text-gray-300">
                <ShoppingCart size={48} />
              </div>
              <h3 className="text-xl font-medium text-gray-500">Chưa có sản phẩm nào được quét</h3>
              <p className="text-gray-400 mt-2 text-center max-w-sm">
                Hãy tiến hành nhập hoặc quét mã SKU ở form bên cạnh để bắt đầu thao tác cập nhật sản phẩm.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Cashier;
