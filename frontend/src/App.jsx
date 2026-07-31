import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import './App.css';

// Auth Provider
import { AuthProvider, useAuth } from './context/AuthContext';

// Pages
import HomePage from './pages/HomePage';
import AdminLogin from './components/Auth/AdminLogin';
import AdminRegister from './components/Auth/AdminRegister';
import UserLogin from './components/Auth/UserLogin';
import UserRegister from './components/Auth/UserRegister';
import AdminDashboard from './components/Admin/AdminDashboard';
import UserDashboard from './components/User/UserDashboard';
import Unauthorized from './pages/Unauthorized';
import NotFound from './pages/NotFound';

// Small dev-only log to force and confirm hot-reload when this file changes
// (will be removed later if undesired)
// eslint-disable-next-line no-console
console.debug('[DEV] App.jsx module loaded');

function AppRoutes() {
  const { isAuthenticated, userRole, loading } = useAuth();
  const navigate = useNavigate();

  // Global handler for unauthorized events emitted by API layer
  // NOTE: Hooks must be called unconditionally at the top level of the component.
  React.useEffect(() => {
    const handler = (e) => {
      console.warn('Unauthorized event received from API', e.detail);
      navigate('/unauthorized');
    };

    window.addEventListener('app:unauthorized', handler);
    return () => window.removeEventListener('app:unauthorized', handler);
  }, [navigate]);

  if (loading) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }

  return (
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<HomePage />} />
        <Route path="/admin/login" element={<AdminLogin />} />
        <Route path="/admin/register" element={<AdminRegister />} />
        <Route path="/user/login" element={<UserLogin />} />
        <Route path="/user/register" element={<UserRegister />} />

        {/* Protected Admin Routes */}
        <Route
          path="/admin/dashboard"
          element={
            isAuthenticated && userRole === 'ADMIN' ? (
              <AdminDashboard />
            ) : (
              <Navigate to="/unauthorized" />
            )
          }
        />

        {/* Protected User Routes */}
        <Route
          path="/user/dashboard"
          element={
            isAuthenticated && userRole === 'USER' ? (
              <UserDashboard />
            ) : (
              <Navigate to="/unauthorized" />
            )
          }
        />

        {/* Error Routes */}
        <Route path="/unauthorized" element={<Unauthorized />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
  );
}

function App() {
  return (
    <AuthProvider>
      <Router>
        <AppRoutes />
      </Router>
    </AuthProvider>
  );
}

export default App;
