import React, { useState } from 'react';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  LayoutDashboard, 
  Settings, 
  Users, 
  Activity, 
  BookOpen, 
  Library, 
  Tag, 
  Flashlight,
  LogOut,
  Menu,
  X,
  Bell,
  Search,
  ChevronLeft,
  ChevronRight,
  UserCheck
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

export const MainLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const hasRole = (roles: string[]) => {
    if (!user?.roles) return false;
    return user.roles.some(r => roles.includes(r));
  };

  const allNavItems = [
    { path: '/dashboard', label: 'Tổng quan', icon: LayoutDashboard, roles: ['ROLE_ADMIN', 'ROLE_LIBRARIAN', 'ROLE_USER'] },
    { path: '/pick-to-light', label: 'Pick-to-Light', icon: Flashlight, roles: ['ROLE_ADMIN', 'ROLE_USER'] },
    { path: '/librarian', label: 'Nghiệp vụ Thủ thư', icon: UserCheck, roles: ['ROLE_ADMIN', 'ROLE_LIBRARIAN'] },
    { path: '/books', label: 'Thư viện Sách', icon: Library, roles: ['ROLE_ADMIN'] },
    { path: '/users', label: 'Người dùng', icon: Users, roles: ['ROLE_ADMIN'] },
    { path: '/mqtt', label: 'Quản lý MQTT', icon: Settings, roles: ['ROLE_ADMIN'] },
    { path: '/sensor-data', label: 'Dữ liệu Cảm biến', icon: Activity, roles: ['ROLE_ADMIN'] },
    { path: '/esl', label: 'Quản lý ESL', icon: Tag, roles: ['ROLE_ADMIN'] },
    { path: '/shelf-map', label: 'Bản đồ Kệ sách', icon: BookOpen, roles: ['ROLE_ADMIN', 'ROLE_LIBRARIAN', 'ROLE_USER'] },
  ];

  const navItems = allNavItems.filter(item => hasRole(item.roles));

  const handleLogout = async () => {
    await logout();
  };

  const currentPathName = navItems.find(item => item.path === location.pathname)?.label || 'Bảng điều khiển';

  return (
    <div className="flex h-screen bg-slate-50 relative overflow-hidden font-sans">
      {/* Background decoration for the whole app */}
      <div className="absolute top-0 w-full h-96 bg-gradient-to-br from-indigo-500/20 via-purple-500/10 to-transparent pointer-events-none -mb-32"></div>
      
      {/* Sidebar background gradient blob */}
      <div className="absolute top-[-10%] left-[-5%] w-96 h-96 bg-indigo-400/20 rounded-full blur-[100px] pointer-events-none"></div>

      {/* Desktop Sidebar */}
      <motion.aside 
        initial={false}
        animate={{ width: isSidebarOpen ? 260 : 80 }}
        className={`hidden md:flex flex-col glass-sidebar z-20 relative transition-all duration-300 ease-in-out`}
      >
        <div className="h-16 flex items-center justify-between px-4 border-b border-indigo-100/50">
          <AnimatePresence mode="wait">
            {isSidebarOpen && (
              <motion.div 
                initial={{ opacity: 0 }} 
                animate={{ opacity: 1 }} 
                exit={{ opacity: 0 }}
                className="flex items-center gap-2 "
              >
                <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-600 to-purple-600 flex items-center justify-center shadow-md">
                  <Activity className="w-5 h-5 text-white" />
                </div>
                <span className="font-bold text-xl text-gray-800 tracking-tight">V-Smart</span>
              </motion.div>
            )}
          </AnimatePresence>
          <button 
            onClick={() => setIsSidebarOpen(!isSidebarOpen)}
            className="p-2 rounded-lg hover:bg-gray-100/50 text-gray-500 transition-colors"
          >
            {isSidebarOpen ? <ChevronLeft className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
          </button>
        </div>

        <div className="flex-1 overflow-y-auto overflow-x-hidden py-6 px-3 flex flex-col gap-1 custom-scrollbar">
          {navItems.map((item) => {
            const isActive = location.pathname.startsWith(item.path);
            return (
              <button
                key={item.path}
                onClick={() => navigate(item.path)}
                className={`flex items-center gap-3 px-3 py-3 rounded-xl transition-all duration-200 group relative ${
                  isActive 
                    ? 'bg-gradient-to-r from-indigo-500/10 to-purple-500/10 text-indigo-700 font-semibold shadow-sm border border-indigo-100' 
                    : 'text-gray-600 hover:bg-white/60 hover:shadow-sm'
                }`}
              >
                {isActive && (
                  <motion.div 
                    layoutId="activeNavIndicator"
                    className="absolute left-0 w-1 h-6 bg-indigo-600 rounded-r-full" 
                  />
                )}
                <item.icon className={`w-5 h-5 flex-shrink-0 ${isActive ? 'text-indigo-600' : 'text-gray-400 group-hover:text-indigo-500'} transition-colors`} />
                <AnimatePresence mode="wait">
                  {isSidebarOpen && (
                    <motion.span 
                      initial={{ opacity: 0, width: 0 }}
                      animate={{ opacity: 1, width: 'auto' }}
                      exit={{ opacity: 0, width: 0 }}
                      className="whitespace-nowrap text-sm"
                    >
                      {item.label}
                    </motion.span>
                  )}
                </AnimatePresence>
              </button>
            )
          })}
        </div>

        <div className="p-4 border-t border-indigo-100/50">
          <button
            onClick={handleLogout}
            className={`flex items-center gap-3 px-3 py-3 rounded-xl w-full text-red-600 hover:bg-red-50 transition-colors ${!isSidebarOpen && 'justify-center'}`}
          >
            <LogOut className="w-5 h-5" />
            {isSidebarOpen && <span className="font-medium text-sm">Đăng xuất</span>}
          </button>
        </div>
      </motion.aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0 z-10">
        {/* Header */}
        <header className="h-16 glass-header flex items-center justify-between px-4 lg:px-8 z-10 sticky top-0">
          <div className="flex items-center gap-4">
            <button 
              className="md:hidden p-2 text-gray-500 hover:bg-gray-100 rounded-lg"
              onClick={() => setIsMobileMenuOpen(true)}
            >
              <Menu className="w-6 h-6" />
            </button>
            <h1 className="text-xl font-bold text-gray-800">{currentPathName}</h1>
          </div>

          <div className="flex items-center gap-3 md:gap-5">
            <div className="hidden md:flex items-center bg-white/50 border border-gray-200/60 rounded-full px-4 py-2 focus-within:ring-2 focus-within:ring-indigo-500/50 focus-within:bg-white transition-all shadow-sm">
              <Search className="w-4 h-4 text-gray-400 mr-2" />
              <input 
                type="text" 
                placeholder="Tìm kiếm..." 
                className="bg-transparent border-none outline-none text-sm w-48 lg:w-64"
              />
            </div>
            
            <button className="p-2 text-gray-500 hover:bg-white/60 hover:text-indigo-600 rounded-full transition-colors relative shadow-sm border border-transparent hover:border-gray-200/60">
              <Bell className="w-5 h-5" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full border border-white"></span>
            </button>

            <div className="h-8 w-[1px] bg-gray-200 mx-1"></div>

            <div className="flex items-center gap-3 pl-1">
              <div className="hidden md:block text-right">
                <p className="text-sm font-semibold text-gray-800">{user?.username}</p>
                <p className="text-xs text-gray-500">{user?.roles?.join(', ')}</p>
              </div>
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 to-purple-500 border-2 border-white shadow-md flex items-center justify-center text-white font-bold cursor-pointer hover:shadow-lg transition-shadow">
                {user?.username?.charAt(0).toUpperCase() || 'U'}
              </div>
            </div>
          </div>
        </header>

        {/* Scrollable Main Area */}
        <main className="flex-1 overflow-auto p-4 lg:p-8 custom-scrollbar relative">
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.2 }}
              className="max-w-7xl mx-auto w-full h-full"
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </main>
      </div>

      {/* Mobile Sidebar Overlay */}
      <AnimatePresence>
        {isMobileMenuOpen && (
          <>
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-gray-900/40 backdrop-blur-sm z-40 md:hidden"
              onClick={() => setIsMobileMenuOpen(false)}
            />
            <motion.div
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', bounce: 0, duration: 0.3 }}
              className="fixed top-0 left-0 bottom-0 w-[280px] bg-white z-50 flex flex-col shadow-2xl md:hidden"
            >
              <div className="h-16 flex items-center justify-between px-6 border-b border-gray-100">
                <span className="font-bold text-xl text-indigo-600">V-Smart</span>
                <button 
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="p-2 text-gray-400 hover:text-gray-600 bg-gray-50 rounded-lg"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
              <div className="flex-1 overflow-y-auto py-4 px-4 flex flex-col gap-1">
                {navItems.map((item) => {
                  const isActive = location.pathname.startsWith(item.path);
                  return (
                    <button
                      key={item.path}
                      onClick={() => {
                        navigate(item.path);
                        setIsMobileMenuOpen(false);
                      }}
                      className={`flex items-center gap-3 px-4 py-3.5 rounded-xl transition-colors ${
                        isActive ? 'bg-indigo-50 text-indigo-700 font-semibold' : 'text-gray-600 hover:bg-gray-50'
                      }`}
                    >
                      <item.icon className={`w-5 h-5 ${isActive ? 'text-indigo-600' : 'text-gray-400'}`} />
                      <span>{item.label}</span>
                    </button>
                  )
                })}
              </div>
              <div className="p-4 border-t border-gray-100">
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-3 px-4 py-3 rounded-xl w-full text-red-600 hover:bg-red-50 transition-colors font-medium"
                >
                  <LogOut className="w-5 h-5" />
                  <span>Đăng xuất</span>
                </button>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
};

export default MainLayout;
