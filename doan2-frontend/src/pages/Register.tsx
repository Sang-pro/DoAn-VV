import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const Register = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const validateForm = (): boolean => {
    if (!formData.username || !formData.email || !formData.password || !formData.confirmPassword) {
      setError('Vui lòng điền tất cả các trường');
      return false;
    }

    if (formData.username.length < 3) {
      setError('Tên đăng nhập phải có ít nhất 3 ký tự');
      return false;
    }

    if (!formData.email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
      setError('Email không hợp lệ');
      return false;
    }

    if (formData.password.length < 6) {
      setError('Mật khẩu phải có ít nhất 6 ký tự');
      return false;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Mật khẩu không khớp');
      return false;
    }

    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      const response = await fetch('http://localhost:8080/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: formData.username,
          email: formData.email,
          password: formData.password,
        }),
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.message || 'Đăng ký thất bại');
      }

      await login(formData.username, formData.password);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || 'Lỗi đăng ký. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex justify-center items-center min-h-screen bg-gradient-to-br from-indigo-600 via-purple-600 to-pink-500 font-sans p-4">
      <div className="bg-white rounded-xl shadow-2xl p-10 w-full max-w-md">
        <h1 className="text-center text-gray-800 mb-8 text-3xl font-bold">Đăng Ký Tài Khoản</h1>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="register-username" className="label">Tên đăng nhập</label>
            <input
              type="text"
              id="register-username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              placeholder="Nhập tên đăng nhập"
              className="input-field"
              disabled={loading}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="register-email" className="label">Email</label>
            <input
              type="email"
              id="register-email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="Nhập email"
              className="input-field"
              disabled={loading}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="register-password" className="label">Mật khẩu</label>
            <input
              type="password"
              id="register-password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Nhập mật khẩu"
              className="input-field"
              disabled={loading}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="register-confirm-password" className="label">Xác nhận mật khẩu</label>
            <input
              type="password"
              id="register-confirm-password"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Xác nhận mật khẩu"
              className="input-field"
              disabled={loading}
              required
            />
          </div>

          {error && <div className="error-box">{error}</div>}

          <button type="submit" className="btn-primary w-full mt-2" disabled={loading}>
            {loading ? 'Đang đăng ký...' : 'Đăng Ký'}
          </button>
        </form>

        <div className="text-center mt-6 text-gray-600 text-sm">
          <p>
            Đã có tài khoản?{' '}
            <Link to="/login" className="text-indigo-600 font-semibold hover:text-purple-600 transition-colors">
              Đăng nhập
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;
