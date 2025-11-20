import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const Login: React.FC = () => {
  const navigate = useNavigate();
  const { login, isLoading, error } = useAuth();
  const [formData, setFormData] = useState({
    username: '',
    password: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const success = await login(formData.username, formData.password);
    if (success) {
      navigate('/dashboard');
    }
  };

  return (
    <div className="flex justify-center items-center min-h-screen bg-gradient-to-br from-indigo-600 via-purple-600 to-pink-500 font-sans">
      <div className="bg-white rounded-xl shadow-2xl p-10 w-full max-w-md">
        <h1 className="text-center text-gray-800 mb-8 text-3xl font-bold">Đăng Nhập</h1>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="login-username" className="label">Tên đăng nhập</label>
            <input
              type="text"
              id="login-username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="Nhập tên đăng nhập"
              className="input-field"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="login-password" className="label">Mật khẩu</label>
            <input
              type="password"
              id="login-password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Nhập mật khẩu"
              className="input-field"
              required
            />
          </div>

          {error && <div className="error-box">{error}</div>}

          <button type="submit" disabled={isLoading} className="btn-primary w-full mt-2">
            {isLoading ? 'Đang xử lý...' : 'Đăng Nhập'}
          </button>
        </form>

        <div className="text-center mt-6 text-gray-600 text-sm">
          <p>
            Chưa có tài khoản?{' '}
            <Link to="/register" className="text-indigo-600 font-semibold hover:text-purple-600 transition-colors">
              Đăng ký tại đây
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
