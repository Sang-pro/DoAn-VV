import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MqttApi } from '../api/mqtt';
import { MqttCredentialsResponse } from '../types';

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
      setDevices(response.data);
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
      mqttPassword: '',
      brokerUrl: device.brokerUrl,
    });
    setEditingId(device.id);
    setShowForm(true);
  };

  const handleDelete = async (id: string) => {
    if (globalThis.confirm('Bạn có chắc chắn muốn xóa thiết bị này?')) {
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
    <div className="min-h-screen bg-gray-100">
      <header className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white shadow-lg">
        <div className="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
          <h1 className="text-3xl font-bold">Quản lý Thiết bị MQTT</h1>
          <button
            className="btn-primary"
            onClick={() => setShowForm(!showForm)}
            disabled={loading}
          >
            {showForm ? 'Hủy' : '+ Thêm Thiết bị'}
          </button>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8 space-y-6">
        {error && <div className="error-box">{error}</div>}

        {showForm && (
          <div className="card p-6">
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="form-group">
                <label htmlFor="mqtt-username" className="label">MQTT Username</label>
                <input
                  type="text"
                  id="mqtt-username"
                  name="mqttUsername"
                  value={formData.mqttUsername}
                  onChange={handleInputChange}
                  placeholder="Nhập MQTT username"
                  className="input-field"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="mqtt-password" className="label">MQTT Password</label>
                <input
                  type="password"
                  id="mqtt-password"
                  name="mqttPassword"
                  value={formData.mqttPassword}
                  onChange={handleInputChange}
                  placeholder="Nhập MQTT password"
                  className="input-field"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="broker-url" className="label">Broker URL</label>
                <input
                  type="text"
                  id="broker-url"
                  name="brokerUrl"
                  value={formData.brokerUrl}
                  onChange={handleInputChange}
                  placeholder="Ví dụ: ssl://broker.example.com:8883"
                  className="input-field"
                />
              </div>

              <div className="flex gap-3">
                <button type="submit" className="btn-primary" disabled={loading}>
                  {loading ? 'Đang lưu...' : editingId ? 'Cập nhật' : 'Thêm mới'}
                </button>
                <button type="button" className="btn-secondary" onClick={handleCancel}>
                  Hủy
                </button>
              </div>
            </form>
          </div>
        )}

        {loading && <div className="text-center text-gray-600 py-8">Đang tải...</div>}

        {!loading && devices.length === 0 && (
          <div className="card p-8 text-center text-gray-500">
            <p>Không có thiết bị MQTT nào. Hãy thêm thiết bị mới.</p>
          </div>
        )}

        {devices.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {devices.map((device) => (
              <div key={device.id} className={`card p-6 ${device.isActive ? 'border-l-4 border-green-500' : 'border-l-4 border-gray-300'}`}>
                <div className="flex justify-between items-start mb-4">
                  <h3 className="font-bold text-gray-800 text-lg">{device.mqttUsername}</h3>
                  <span className={`text-xs font-semibold px-3 py-1 rounded-full ${device.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
                    {device.isActive ? 'Hoạt động' : 'Không hoạt động'}
                  </span>
                </div>

                <div className="space-y-2 mb-4 text-sm">
                  <div>
                    <label className="font-semibold text-gray-700">Broker URL:</label>
                    <code className="block bg-gray-100 p-2 rounded mt-1 text-xs overflow-auto">{device.brokerUrl}</code>
                  </div>

                  <div>
                    <label className="font-semibold text-gray-700">Ngày tạo:</label>
                    <span className="block text-gray-600">{new Date(device.createdAt || '').toLocaleString('vi-VN')}</span>
                  </div>

                  <div>
                    <label className="font-semibold text-gray-700">Cập nhật lần cuối:</label>
                    <span className="block text-gray-600">{new Date(device.updatedAt || '').toLocaleString('vi-VN')}</span>
                  </div>
                </div>

                <div className="flex gap-2 flex-wrap">
                  <button
                    className="flex-1 px-3 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 transition-colors"
                    onClick={() => handleEdit(device)}
                  >
                    Sửa
                  </button>

                  {device.isActive && (
                    <button
                      className="flex-1 px-3 py-2 bg-yellow-600 text-white text-sm rounded hover:bg-yellow-700 transition-colors"
                      onClick={() => handleDeactivate(device.id)}
                    >
                      Tắt
                    </button>
                  )}

                  <button
                    className="flex-1 px-3 py-2 bg-red-600 text-white text-sm rounded hover:bg-red-700 transition-colors"
                    onClick={() => handleDelete(device.id)}
                  >
                    Xóa
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
};
