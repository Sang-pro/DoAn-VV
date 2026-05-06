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
    if (!isConnected) return;

    // Subscribe to all smarttrash topics
    const unsubscribe = subscribe('smarttrash', (message) => {
      try {
        const payload = message.message;

        // Parse if it's a string
        const data = typeof payload === 'string' ? JSON.parse(payload) : payload;

        const reading: SensorReading = {
          id: `${Date.now()}-${data.n || 'unknown'}`,
          timestamp: new Date().toLocaleString('vi-VN'),
          other: data, // Store all data in 'other'
        };

        setSensorReadings((prev) => [reading, ...prev.slice(0, 99)]);
      } catch (err) {
        console.error('Error processing sensor message:', err);
      }
    });

    return unsubscribe;
  }, [isConnected, subscribe]);

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



  const getLatestValue = (key: string) => {
    if (sensorReadings.length === 0) return null;
    const latestReading = sensorReadings[0];

    // Check if key exists in 'other' object first (where our ESP32 data lives)
    if (latestReading.other && key in latestReading.other) {
      return latestReading.other[key];
    }

    // Fallback to top-level properties
    return (latestReading as any)[key];
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
                    <div className="text-sm font-semibold opacity-90">Mức rác</div>
                    <div className="text-3xl font-bold mt-2">
                      {getLatestValue('trash') === undefined
                        ? 'N/A'
                        : `${getLatestValue('trash')}%`}
                    </div>
                  </div>

                  <div className="card p-6 bg-gradient-to-br from-blue-400 to-blue-500 text-white">
                    <div className="text-sm font-semibold opacity-90">Cảm biến Gas</div>
                    <div className="text-3xl font-bold mt-2">
                      {getLatestValue('g') === undefined
                        ? 'N/A'
                        : String(getLatestValue('g'))}
                    </div>
                  </div>
                    <div className="card p-6 bg-gradient-to-br from-purple-400 to-purple-500 text-white">
                    <div className="text-sm font-semibold opacity-90">Vị trí GPS</div>
                    <div className="text-lg font-bold mt-2">
                      {getLatestValue('lat') === undefined
                        ? 'N/A'
                        : `${Number(getLatestValue('lat')).toFixed(4)}, ${Number(getLatestValue('lon')).toFixed(4)}`}
                    </div>
                  </div>

                  <div className="card p-6 bg-gradient-to-br from-yellow-400 to-yellow-500 text-white">
                    <div className="text-sm font-semibold opacity-90">Gia tốc (Z)</div>
                    <div className="text-3xl font-bold mt-2">
                      {getLatestValue('az') === undefined
                        ? 'N/A'
                        : Number(getLatestValue('az')).toFixed(2)}
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
                          <th className="px-4 py-2 text-right">Mức rác</th>
                          <th className="px-4 py-2 text-right">Gas</th>
                          <th className="px-4 py-2 text-right">GPS (Lat, Lon)</th>
                          <th className="px-4 py-2 text-right">Gia tốc (X, Y, Z)</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y">
                        {sensorReadings.map((reading: any) => (
                          <tr key={reading.id} className="hover:bg-gray-50">
                            <td className="px-4 py-3 text-gray-700">{reading.timestamp}</td>
                            <td className="px-4 py-3 text-right text-gray-700">
                              {reading.other?.trash !== undefined ? `${reading.other.trash}%` : '-'}
                            </td>
                            <td className="px-4 py-3 text-right text-gray-700">
                              {reading.other?.g !== undefined ? reading.other.g : '-'}
                            </td>
                            <td className="px-4 py-3 text-right text-gray-700">
                              {reading.other?.lat !== undefined
                                ? `${Number(reading.other.lat).toFixed(4)}, ${Number(reading.other.lon).toFixed(4)}`
                                : '-'}
                            </td>
                            <td className="px-4 py-3 text-right text-gray-700">
                              {reading.other?.ax !== undefined
                              ? `${Number(reading.other.ax).toFixed(2)}, ${Number(reading.other.ay).toFixed(2)}, ${Number(reading.other.az).toFixed(2)}`
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
