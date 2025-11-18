import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useWebSocket } from '../hooks/useWebSocket';
import { mqttAPI } from '../api/mqtt';
import './SensorDataView.css';

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

  // Load active devices on mount
  useEffect(() => {
    loadDevices();
  }, []);

  // Subscribe to WebSocket messages when device selected
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
    <div className="sensor-data-view">
      <div className="sensor-header">
        <h1>Dữ liệu Cảm biến Realtime</h1>
        <div className="connection-status">
          <span className={`status-indicator ${isConnected ? 'connected' : 'disconnected'}`}></span>
          <span>{isConnected ? 'Kết nối' : 'Ngắt kết nối'}</span>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {loading && <div className="loading">Đang tải...</div>}

      {!loading && devices.length === 0 && (
        <div className="empty-state">
          <p>Không có thiết bị MQTT nào. Vui lòng thêm thiết bị từ trang Quản lý MQTT.</p>
        </div>
      )}

      {!loading && devices.length > 0 && (
        <>
          <div className="device-selector">
            <label htmlFor="device-select">Chọn thiết bị:</label>
            <select
              id="device-select"
              value={selectedDevice}
              onChange={(e) => handleDeviceChange(e.target.value)}
            >
              {devices.map((device) => (
                <option key={device.id} value={device.id}>
                  {device.mqttUsername}
                </option>
              ))}
            </select>
          </div>

          <div className="sensor-data-container">
            {sensorReadings.length === 0 ? (
              <div className="waiting-data">
                <p>Chờ dữ liệu từ cảm biến...</p>
              </div>
            ) : (
              <>
                <div className="sensor-metrics">
                  <div className="metric-card temperature">
                    <div className="metric-label">Nhiệt độ</div>
                    <div className="metric-value">
                      {getLatestValue('temperature') === null
                        ? 'N/A'
                        : formatValue(getLatestValue('temperature'), 'temperature')}
                    </div>
                  </div>

                  <div className="metric-card humidity">
                    <div className="metric-label">Độ ẩm</div>
                    <div className="metric-value">
                      {getLatestValue('humidity') === null
                        ? 'N/A'
                        : formatValue(getLatestValue('humidity'), 'humidity')}
                    </div>
                  </div>

                  <div className="metric-card pressure">
                    <div className="metric-label">Áp suất</div>
                    <div className="metric-value">
                      {getLatestValue('pressure') === null
                        ? 'N/A'
                        : formatValue(getLatestValue('pressure'), 'pressure')}
                    </div>
                  </div>

                  <div className="metric-card light">
                    <div className="metric-label">Ánh sáng</div>
                    <div className="metric-value">
                      {getLatestValue('light') === null
                        ? 'N/A'
                        : formatValue(getLatestValue('light'), 'light')}
                    </div>
                  </div>
                </div>

                <div className="sensor-readings">
                  <h2>Lịch sử dữ liệu (100 bản ghi gần nhất)</h2>
                  <div className="readings-table">
                    <div className="table-header">
                      <div className="col-timestamp">Thời gian</div>
                      <div className="col-temperature">Nhiệt độ</div>
                      <div className="col-humidity">Độ ẩm</div>
                      <div className="col-pressure">Áp suất</div>
                      <div className="col-light">Ánh sáng</div>
                      <div className="col-data">Dữ liệu khác</div>
                    </div>
                    <div className="table-body">
                      {sensorReadings.map((reading) => (
                        <div key={reading.id} className="table-row">
                          <div className="col-timestamp">{reading.timestamp}</div>
                          <div className="col-temperature">
                            {reading.temperature === undefined
                              ? '-'
                              : formatValue(reading.temperature, 'temperature')}
                          </div>
                          <div className="col-humidity">
                            {reading.humidity === undefined
                              ? '-'
                              : formatValue(reading.humidity, 'humidity')}
                          </div>
                          <div className="col-pressure">
                            {reading.pressure === undefined
                              ? '-'
                              : formatValue(reading.pressure, 'pressure')}
                          </div>
                          <div className="col-light">
                            {reading.light === undefined
                              ? '-'
                              : formatValue(reading.light, 'light')}
                          </div>
                          <div className="col-data">
                            {reading.other && Object.keys(reading.other).length > 0
                              ? JSON.stringify(reading.other)
                              : '-'}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </>
            )}
          </div>
        </>
      )}
    </div>
  );
};
