import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useWebSocket } from '../hooks/useWebSocket';
import { mqttAPI } from '../api/mqtt';

interface SensorReading {
  id?: string;
  timestamp: string;
  temperature?: number;
  humidity?: number;
  pressure?: number;
  light?: number;
  motion?: boolean;
  other?: Record<string, any>;
}

export const SensorDataView = () => {
  const navigate = useNavigate();
  const { subscribe, isConnected } = useWebSocket();
  const [selectedDevice, setSelectedDevice] = useState<string>('');
  const [devices, setDevices] = useState<any[]>([]);
  const [sensorReadings, setSensorReadings] = useState<SensorReading[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadDevices();
  }, []);

  useEffect(() => {
    if (!selectedDevice || !isConnected) return;

    const topic = `data/${selectedDevice}/sensors`;
    const unsubscribe = subscribe(topic, (message) => {
      try {
        const reading: SensorReading = {
          id: `${Date.now()}-${selectedDevice}`,
          timestamp: new Date().toLocaleString('vi-VN'),
          ...message.message,
        };
        setSensorReadings((prev) => [reading, ...prev.slice(0, 99)]);
      } catch (err) {
        console.error('Error processing sensor message:', err);
      }
    });

    return unsubscribe;
  }, [selectedDevice, isConnected, subscribe]);

  const loadDevices = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await mqttAPI.getActive();
      setDevices(response.data);
      if (response.data.length > 0) {
        setSelectedDevice(response.data[0].id);
      }
    } catch (err: any) {
      if (err.response?.status === 401) {
        navigate('/login');
      } else {
        setError('Lỗi khi tải danh sách thiết bị');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDeviceChange = (deviceId: string) => {
    setSelectedDevice(deviceId);
    setSensorReadings([]);
  };

  const formatValue = (value: any, key: string): string => {
    if (typeof value === 'number') {
      if (key.toLowerCase().includes('temp')) {
        return `${value.toFixed(2)}°C`;
      } else if (key.toLowerCase().includes('humid')) {
        return `${value.toFixed(1)}%`;
      } else if (key.toLowerCase().includes('press')) {
        return `${value.toFixed(2)} hPa`;
      } else if (key.toLowerCase().includes('light')) {
        return `${value.toFixed(0)} lux`;
      }
      return value.toFixed(2);
    } else if (typeof value === 'boolean') {
      return value ? 'Có' : 'Không';
    }
    return String(value);
  };

  const getLatestValue = (key: string) => {
    if (sensorReadings.length === 0) return null;
    const latestReading = sensorReadings[0];
    return latestReading[key as keyof SensorReading];
  };

  return (
    <div className="min-h-screen bg-gray-100">
      <header className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white shadow-lg">
        <div className="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
          <h1 className="text-3xl font-bold">Dữ liệu Cảm biến Realtime</h1>
          <div className="flex items-center gap-2">
            <div className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-400' : 'bg-red-400'}`}></div>
            <span>{isConnected ? 'Kết nối' : 'Ngắt kết nối'}</span>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8 space-y-6">
        {error && <div className="error-box">{error}</div>}

        {loading && <div className="text-center text-gray-600 py-8">Đang tải...</div>}

        {!loading && devices.length === 0 && (
          <div className="card p-8 text-center text-gray-500">
            <p>Không có thiết bị MQTT nào. Vui lòng thêm thiết bị từ trang Quản lý MQTT.</p>
          </div>
        )}

        {!loading && devices.length > 0 && (
          <>
            <div className="card p-6">
              <label htmlFor="device-select" className="label block mb-2">Chọn thiết bị:</label>
              <select
                id="device-select"
                value={selectedDevice}
                onChange={(e) => handleDeviceChange(e.target.value)}
                className="input-field"
              >
                {devices.map((device) => (
                  <option key={device.id} value={device.id}>
                    {device.mqttUsername}
                  </option>
                ))}
              </select>
            </div>

            {sensorReadings.length === 0 ? (
              <div className="card p-8 text-center text-gray-500">
                <p>Chờ dữ liệu từ cảm biến...</p>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="card p-6 bg-gradient-to-br from-orange-400 to-orange-500 text-white">
                    <div className="text-sm font-semibold opacity-90">Nhiệt độ</div>
                    <div className="text-3xl font-bold mt-2">
                      {getLatestValue('temperature') === null
                        ? 'N/A'
                        : formatValue(getLatestValue('temperature'), 'temperature')}
                    </div>
                  </div>

                  <div className="card p-6 bg-gradient-to-br from-blue-400 to-blue-500 text-white">
                    <div className="text-sm font-semibold opacity-90">Độ ẩm</div>
                    <div className="text-3xl font-bold mt-2">
                      {getLatestValue('humidity') === null
                        ? 'N/A'
                        : formatValue(getLatestValue('humidity'), 'humidity')}
                    </div>
                  </div>

                  <div className="card p-6 bg-gradient-to-br from-purple-400 to-purple-500 text-white">
                    <div className="text-sm font-semibold opacity-90">Áp suất</div>
                    <div className="text-3xl font-bold mt-2">
                      {getLatestValue('pressure') === null
                        ? 'N/A'
                        : formatValue(getLatestValue('pressure'), 'pressure')}
                    </div>
                  </div>

                  <div className="card p-6 bg-gradient-to-br from-yellow-400 to-yellow-500 text-white">
                    <div className="text-sm font-semibold opacity-90">Ánh sáng</div>
                    <div className="text-3xl font-bold mt-2">
                      {getLatestValue('light') === null
                        ? 'N/A'
                        : formatValue(getLatestValue('light'), 'light')}
                    </div>
                  </div>
                </div>

                <div className="card p-6">
                  <h2 className="text-xl font-bold text-gray-800 mb-4">Lịch sử dữ liệu (100 bản ghi gần nhất)</h2>
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="bg-gray-200 text-gray-800">
                          <th className="px-4 py-2 text-left">Thời gian</th>
                          <th className="px-4 py-2 text-right">Nhiệt độ</th>
                          <th className="px-4 py-2 text-right">Độ ẩm</th>
                          <th className="px-4 py-2 text-right">Áp suất</th>
                          <th className="px-4 py-2 text-right">Ánh sáng</th>
                          <th className="px-4 py-2 text-left">Dữ liệu khác</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y">
                        {sensorReadings.map((reading) => (
                          <tr key={reading.id} className="hover:bg-gray-50">
                            <td className="px-4 py-3 text-gray-700">{reading.timestamp}</td>
                            <td className="px-4 py-3 text-right text-gray-700">
                              {reading.temperature === undefined
                                ? '-'
                                : formatValue(reading.temperature, 'temperature')}
                            </td>
                            <td className="px-4 py-3 text-right text-gray-700">
                              {reading.humidity === undefined
                                ? '-'
                                : formatValue(reading.humidity, 'humidity')}
                            </td>
                            <td className="px-4 py-3 text-right text-gray-700">
                              {reading.pressure === undefined
                                ? '-'
                                : formatValue(reading.pressure, 'pressure')}
                            </td>
                            <td className="px-4 py-3 text-right text-gray-700">
                              {reading.light === undefined
                                ? '-'
                                : formatValue(reading.light, 'light')}
                            </td>
                            <td className="px-4 py-3 text-gray-700 font-mono text-xs">
                              {reading.other && Object.keys(reading.other).length > 0
                                ? JSON.stringify(reading.other)
                                : '-'}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </>
            )}
          </>
        )}
      </main>
    </div>
  );
};
