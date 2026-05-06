import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { mqttAPI } from '../api/mqtt';
import { MqttCredentialsResponse } from '../types';

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [mqttDevices, setMqttDevices] = React.useState<MqttCredentialsResponse[]>([]);
  const [brokerStatus, setBrokerStatus] = React.useState({ isConnected: false });
  const [loading, setLoading] = React.useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [devicesRes, statusRes] = await Promise.all([
        mqttAPI.getActive(),
        mqttAPI.getBrokerStatus(),
      ]);
      setMqttDevices(devicesRes.data);
      setBrokerStatus(statusRes.data);
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      setLoading(false);
    }
  };


  const renderDevicesList = () => {
    if (loading) {
      return (
        <div className="flex justify-center p-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
        </div>
      );
    }
    if (mqttDevices.length === 0) {
      return <p className="text-gray-500 italic text-center py-4">Không có thiết bị nào đang hoạt động</p>;
    }
    return (
      <div className="space-y-4">
        {mqttDevices.slice(0, 5).map((device) => (
          <div key={device.id} className="glass border border-gray-100 p-5 rounded-xl flex justify-between items-center group transition-all hover:scale-[1.01] hover:shadow-lg">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 rounded-full bg-indigo-50 flex flex-shrink-0 items-center justify-center">
                <span className="text-xl">📡</span>
              </div>
              <div className="flex-1">
                <h3 className="font-semibold text-gray-800 group-hover:text-indigo-600 transition-colors uppercase tracking-wide text-sm">{device.mqttUsername}</h3>
                <p className="text-xs text-gray-500 mt-1 font-mono">{device.brokerUrl}</p>
                <p className="text-xs text-gray-400 mt-1">Tạo: {new Date(device.createdAt).toLocaleString('vi-VN')}</p>
              </div>
            </div>
            {device.isActive && (
              <span className="bg-green-100/80 text-green-700 px-4 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider backdrop-blur-sm border border-green-200">
                Online
              </span>
            )}
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="space-y-6 animate-in slide-in-from-bottom-4 duration-500 fade-in">
      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="glass-panel p-6 border-t-4 border-t-indigo-500 relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity transform group-hover:scale-110 duration-500">
            <span className="text-6xl">💻</span>
          </div>
          <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">Tổng thiết bị</p>
          <h3 className="text-3xl font-bold text-gray-800 mt-2">{mqttDevices.length}</h3>
          <p className="text-xs text-green-600 mt-2 font-medium flex items-center gap-1">
            <span>↑ Tăng 2%</span> <span className="text-gray-400 font-normal">so với tuần trước</span>
          </p>
        </div>

        <div className={`glass-panel p-6 border-t-4 ${brokerStatus.isConnected ? 'border-t-green-500' : 'border-t-red-500'} relative overflow-hidden group`}>
          <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity transform group-hover:scale-110 duration-500">
            <span className="text-6xl">🔌</span>
          </div>
          <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">MQTT Broker</p>
          <div className="flex items-center gap-3 mt-2">
            <div className={`w-3 h-3 rounded-full animate-pulse ${brokerStatus.isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
            <h3 className={`text-xl font-bold ${brokerStatus.isConnected ? 'text-green-600' : 'text-red-600'}`}>
              {brokerStatus.isConnected ? 'Đã kết nối' : 'Mất kết nối'}
            </h3>
          </div>
          <p className="text-xs text-gray-400 mt-2">Cập nhật lúc: {new Date().toLocaleTimeString('vi-VN')}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="glass-panel p-6 lg:col-span-2 shadow-sm border border-gray-100">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h2 className="text-lg font-bold text-gray-800">Thiết Bị MQTT Đang Hoạt Động</h2>
              <p className="text-sm text-gray-500 mt-1">Danh sách thiết bị kết nối thời gian thực</p>
            </div>
            <button
              className="text-sm bg-indigo-50 text-indigo-600 hover:bg-indigo-100 px-4 py-2 rounded-lg font-semibold transition-colors"
              onClick={() => navigate('/mqtt')}
            >
              Xem tất cả
            </button>
          </div>
          {renderDevicesList()}
        </div>
        
        <div className="glass-panel p-6 shadow-sm border border-gray-100 bg-gradient-to-br from-white to-indigo-50/50">
          <h2 className="text-lg font-bold text-gray-800 mb-6">Hành động khởi xướng</h2>
          <div className="space-y-3">
            <button
              onClick={() => navigate('/sensor-data')}
              className="w-full flex items-center justify-between p-4 rounded-xl border border-gray-200 bg-white hover:border-indigo-300 hover:shadow-md transition-all group"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-blue-100 text-blue-600 flex items-center justify-center group-hover:scale-110 transition-transform">
                  📈
                </div>
                <span className="font-semibold text-gray-700">Xem Dữ liệu Cảm biến</span>
              </div>
              <span className="text-gray-400 group-hover:text-indigo-500 transition-colors">→</span>
            </button>
            <button
              onClick={() => navigate('/products')}
              className="w-full flex items-center justify-between p-4 rounded-xl border border-gray-200 bg-white hover:border-indigo-300 hover:shadow-md transition-all group"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-orange-100 text-orange-600 flex items-center justify-center group-hover:scale-110 transition-transform">
                  📦
                </div>
                <span className="font-semibold text-gray-700">Quản lý Sản phẩm</span>
              </div>
              <span className="text-gray-400 group-hover:text-indigo-500 transition-colors">→</span>
            </button>
            <button
              onClick={() => navigate('/esl')}
              className="w-full flex items-center justify-between p-4 rounded-xl border border-gray-200 bg-white hover:border-indigo-300 hover:shadow-md transition-all group"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-teal-100 text-teal-600 flex items-center justify-center group-hover:scale-110 transition-transform">
                  🏷️
                </div>
                <span className="font-semibold text-gray-700">Cập nhật tem ESL</span>
              </div>
              <span className="text-gray-400 group-hover:text-indigo-500 transition-colors">→</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
