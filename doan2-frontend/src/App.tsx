import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import Dashboard from './pages/Dashboard';
import { MqttManager } from './pages/MqttManager';
import { SensorDataView } from './pages/SensorDataView';
import ProtectedRoute from './components/ProtectedRoute';
import RoleProtectedRoute from './components/RoleProtectedRoute';
import UserManagement from './pages/UserManagement';
import BookCatalog from './pages/BookCatalog';
import EslManagement from './pages/EslManagement';
import PickToLight from './pages/PickToLight';
import ShelfMap from './pages/ShelfMap';
import Librarian from './pages/Librarian';
import BorrowHistory from './pages/BorrowHistory';
import RfidManagement from './pages/RfidManagement';
import AIChatbot from './components/AIChatbot';
import MainLayout from './components/MainLayout';
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
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/pick-to-light" element={<PickToLight />} />
          
          <Route path="/mqtt" element={
            <RoleProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <MqttManager />
            </RoleProtectedRoute>
          } />
          <Route path="/sensor-data" element={
            <RoleProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <SensorDataView />
            </RoleProtectedRoute>
          } />
          <Route path="/users" element={
            <RoleProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <UserManagement />
            </RoleProtectedRoute>
          } />
          <Route path="/books" element={
            <RoleProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <BookCatalog />
            </RoleProtectedRoute>
          } />
          <Route path="/esl" element={
            <RoleProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <EslManagement />
            </RoleProtectedRoute>
          } />
          <Route path="/rfid" element={
            <RoleProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_LIBRARIAN']}>
              <RfidManagement />
            </RoleProtectedRoute>
          } />
          <Route path="/shelf-map" element={
            <RoleProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_USER', 'ROLE_LIBRARIAN']}>
              <ShelfMap />
            </RoleProtectedRoute>
          } />
          <Route path="/librarian" element={
            <RoleProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_LIBRARIAN']}>
              <Librarian />
            </RoleProtectedRoute>
          } />
          <Route path="/borrow-history" element={
            <RoleProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_USER', 'ROLE_LIBRARIAN']}>
              <BorrowHistory />
            </RoleProtectedRoute>
          } />
        </Route>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Routes>
      <AIChatbot />
    </BrowserRouter>
  );
}

export default App;
