import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { mqttAPI } from '../api/mqtt';
import { MqttCredentialsResponse } from '../types';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
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

  const handleLogout = async () => {
    await logout();
  };

  const renderDevicesList = () => {
    if (loading) {
      return <p>Đang tải dữ liệu...</p>;
    }
    if (mqttDevices.length === 0) {
      return <p>Không có thiết bị nào</p>;
    }
    return (
      <div className="devices-list">
        {mqttDevices.slice(0, 5).map((device) => (
          <div key={device.id} className="device-item">
            <div className="device-info">
              <h3>{device.mqttUsername}</h3>
              <p>Broker: {device.brokerUrl}</p>
              <p>Tạo lúc: {new Date(device.createdAt).toLocaleString('vi-VN')}</p>
            </div>
            <div className="device-status">
              {device.isActive && <span className="active-badge">Hoạt động</span>}
            </div>
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>Dashboard</h1>
        <div className="header-right">
          <span className="username">Xin chào, {user?.username}</span>
          <button onClick={handleLogout} className="logout-btn">
            Đăng Xuất
          </button>
        </div>
      </header>

      <nav className="dashboard-nav">
        <button
          className="nav-btn"
          onClick={() => navigate('/dashboard')}
        >
          📊 Tổng quan
        </button>
        <button
          className="nav-btn"
          onClick={() => navigate('/mqtt')}
        >
          🔧 Quản lý MQTT
        </button>
        <button
          className="nav-btn"
          onClick={() => navigate('/sensor-data')}
        >
          📈 Dữ liệu Cảm biến
        </button>
      </nav>

      <main className="dashboard-main">
        <div className="status-card">
          <h2>Trạng Thái Broker MQTT</h2>
          <div className={`status-indicator ${brokerStatus.isConnected ? 'connected' : 'disconnected'}`}>
            <span className="status-dot"></span>
            <span>{brokerStatus.isConnected ? 'Đã kết nối' : 'Chưa kết nối'}</span>
          </div>
        </div>

        <div className="devices-card">
          <div className="card-header">
            <h2>Thiết Bị MQTT Đang Hoạt Động</h2>
            <button
              className="view-all-btn"
              onClick={() => navigate('/mqtt')}
            >
              Xem tất cả →
            </button>
          </div>
          {renderDevicesList()}
        </div>
      </main>
    </div>
  );
};

export default Dashboard;
