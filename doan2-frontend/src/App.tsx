import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import { MqttManager } from './pages/MqttManager';
import { SensorDataView } from './pages/SensorDataView';
import ProtectedRoute from './components/ProtectedRoute';
import UserManagement from './pages/UserManagement';
import AIHub from './pages/AIHub';
import './App.css';

function App() {
  useEffect(() => {
    const store = useAuthStore.getState();
    store.loadFromStorage();
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/mqtt"
          element={
            <ProtectedRoute>
              <MqttManager />
            </ProtectedRoute>
          }
        />
        <Route
          path="/sensor-data"
          element={
            <ProtectedRoute>
              <SensorDataView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/users"
          element={
            <ProtectedRoute>
              <UserManagement />
            </ProtectedRoute>
          }
        />
        <Route
          path="/ai-hub"
          element={
            <ProtectedRoute>
              <AIHub />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
