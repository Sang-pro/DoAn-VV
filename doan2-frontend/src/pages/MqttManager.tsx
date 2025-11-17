import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MqttApi } from '../api/mqtt';
import { MqttCredentialsResponse } from '../types';
import './MqttManager.css';

export const MqttManager = () => {
  const navigate = useNavigate();
  const [devices, setDevices] = useState<MqttCredentialsResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    mqttUsername: '',
    mqttPassword: '',
    brokerUrl: '',
  });

  useEffect(() => {
    loadDevices();
  }, []);

  const loadDevices = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await MqttApi.getActive();
      setDevices(response);
    } catch (err: any) {
      if (err.response?.status === 401) {
        navigate('/login');
      } else {
        setError(err.response?.data?.message || 'Lỗi khi tải danh sách thiết bị');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!formData.mqttUsername || !formData.mqttPassword) {
      setError('Vui lòng nhập username và password');
      return;
    }

    try {
      if (editingId) {
        await MqttApi.update(editingId, formData);
      } else {
        await MqttApi.register(formData);
      }
      setFormData({ mqttUsername: '', mqttPassword: '', brokerUrl: '' });
      setShowForm(false);
      setEditingId(null);
      loadDevices();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Lỗi khi lưu thiết bị');
    }
  };

  const handleEdit = (device: MqttCredentialsResponse) => {
    setFormData({
      mqttUsername: device.mqttUsername,
      mqttPassword: device.mqttPassword || '',
      brokerUrl: device.brokerUrl,
    });
    setEditingId(device.id);
    setShowForm(true);
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa thiết bị này?')) {
      try {
        await MqttApi.delete(id);
        loadDevices();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Lỗi khi xóa thiết bị');
      }
    }
  };

  const handleDeactivate = async (id: string) => {
    try {
      await MqttApi.deactivate(id);
      loadDevices();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Lỗi khi tắt thiết bị');
    }
  };

  const handleCancel = () => {
    setFormData({ mqttUsername: '', mqttPassword: '', brokerUrl: '' });
    setShowForm(false);
    setEditingId(null);
  };

  return (
    <div className="mqtt-manager">
      <div className="mqtt-header">
        <h1>Quản lý Thiết bị MQTT</h1>
        <button
          className="btn btn-primary"
          onClick={() => setShowForm(!showForm)}
          disabled={loading}
        >
          {showForm ? 'Hủy' : '+ Thêm Thiết bị'}
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {showForm && (
        <div className="mqtt-form-container">
          <form className="mqtt-form" onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="mqttUsername">MQTT Username</label>
              <input
                type="text"
                id="mqttUsername"
                name="mqttUsername"
                value={formData.mqttUsername}
                onChange={handleInputChange}
                placeholder="Nhập MQTT username"
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="mqttPassword">MQTT Password</label>
              <input
                type="password"
                id="mqttPassword"
                name="mqttPassword"
                value={formData.mqttPassword}
                onChange={handleInputChange}
                placeholder="Nhập MQTT password"
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="brokerUrl">Broker URL</label>
              <input
                type="text"
                id="brokerUrl"
                name="brokerUrl"
                value={formData.brokerUrl}
                onChange={handleInputChange}
                placeholder="Ví dụ: ssl://broker.example.com:8883"
              />
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-success" disabled={loading}>
                {loading ? 'Đang lưu...' : editingId ? 'Cập nhật' : 'Thêm mới'}
              </button>
              <button type="button" className="btn btn-secondary" onClick={handleCancel}>
                Hủy
              </button>
            </div>
          </form>
        </div>
      )}

      {loading && <div className="loading">Đang tải...</div>}

      {!loading && devices.length === 0 && (
        <div className="empty-state">
          <p>Không có thiết bị MQTT nào. Hãy thêm thiết bị mới.</p>
        </div>
      )}

      {!loading && devices.length > 0 && (
        <div className="mqtt-devices-grid">
          {devices.map((device) => (
            <div key={device.id} className={`mqtt-device-card ${device.isActive ? 'active' : 'inactive'}`}>
              <div className="device-header">
                <h3>{device.mqttUsername}</h3>
                <span className={`status-badge ${device.isActive ? 'active' : 'inactive'}`}>
                  {device.isActive ? 'Hoạt động' : 'Không hoạt động'}
                </span>
              </div>

              <div className="device-details">
                <div className="detail-item">
                  <label>Broker URL:</label>
                  <code>{device.brokerUrl}</code>
                </div>

                <div className="detail-item">
                  <label>Ngày tạo:</label>
                  <span>{new Date(device.createdAt).toLocaleString('vi-VN')}</span>
                </div>

                <div className="detail-item">
                  <label>Cập nhật lần cuối:</label>
                  <span>{new Date(device.updatedAt).toLocaleString('vi-VN')}</span>
                </div>
              </div>

              <div className="device-actions">
                <button
                  className="btn btn-small btn-info"
                  onClick={() => handleEdit(device)}
                >
                  Sửa
                </button>

                {device.isActive && (
                  <button
                    className="btn btn-small btn-warning"
                    onClick={() => handleDeactivate(device.id)}
                  >
                    Tắt
                  </button>
                )}

                <button
                  className="btn btn-small btn-danger"
                  onClick={() => handleDelete(device.id)}
                >
                  Xóa
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
