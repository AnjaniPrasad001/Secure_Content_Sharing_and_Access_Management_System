import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userRole, setUserRole] = useState(null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [phoneNumberWarning, setPhoneNumberWarning] = useState(null);

  // Initialize auth state from localStorage
  const initializeAuth = useCallback(() => {
    try {
      const token = localStorage.getItem('token');
      const userData = localStorage.getItem('user');
      const warning = localStorage.getItem('phoneNumberWarning');
      
      if (token && userData) {
        const parsedUser = JSON.parse(userData);
        setUser(parsedUser);
        setUserRole(parsedUser.role);
        setIsAuthenticated(true);
        
        if (warning) {
          setPhoneNumberWarning(warning);
        }
      } else {
        setIsAuthenticated(false);
        setUserRole(null);
        setUser(null);
        setPhoneNumberWarning(null);
      }
    } catch (error) {
      console.error('Error initializing auth:', error);
      setIsAuthenticated(false);
      setUserRole(null);
      setUser(null);
      setPhoneNumberWarning(null);
    } finally {
      setLoading(false);
    }
  }, []);

  // Initialize on mount
  useEffect(() => {
    initializeAuth();
  }, [initializeAuth]);

  // Listen for storage changes (cross-tab sync)
  useEffect(() => {
    const handleStorageChange = (e) => {
      if (e.key === 'token' || e.key === 'user' || e.key === 'phoneNumberWarning') {
        initializeAuth();
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [initializeAuth]);

  const login = useCallback((userData) => {
    try {
      // Only persist token if present
      if (userData?.token) {
        localStorage.setItem('token', userData.token);
      } else {
        localStorage.removeItem('token');
      }
      if (userData) {
        localStorage.setItem('user', JSON.stringify(userData));
      }
      
      // Store phone number warning if present
      if (userData.phoneNumberWarning) {
        localStorage.setItem('phoneNumberWarning', userData.phoneNumberWarning);
        setPhoneNumberWarning(userData.phoneNumberWarning);
      } else {
        localStorage.removeItem('phoneNumberWarning');
        setPhoneNumberWarning(null);
      }
      
      // Only mark authenticated when a valid token exists
      if (userData?.token) {
        setUser(userData);
        setUserRole(userData.role);
        setIsAuthenticated(true);
      } else {
        setUser(null);
        setUserRole(null);
        setIsAuthenticated(false);
      }
      
      // Dispatch custom event for same-tab synchronization
      window.dispatchEvent(new CustomEvent('authStateChange', { detail: { type: 'login', userData } }));
    } catch (error) {
      console.error('Error during login:', error);
    }
  }, []);

  const logout = useCallback(() => {
    try {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      localStorage.removeItem('phoneNumberWarning');
      
      setUser(null);
      setUserRole(null);
      setIsAuthenticated(false);
      setPhoneNumberWarning(null);
      
      // Dispatch custom event
      window.dispatchEvent(new CustomEvent('authStateChange', { detail: { type: 'logout' } }));
    } catch (error) {
      console.error('Error during logout:', error);
    }
  }, []);

  const clearPhoneNumberWarning = useCallback(() => {
    setPhoneNumberWarning(null);
    localStorage.removeItem('phoneNumberWarning');
  }, []);

  const value = {
    isAuthenticated,
    userRole,
    user,
    loading,
    phoneNumberWarning,
    login,
    logout,
    clearPhoneNumberWarning,
    reinitialize: initializeAuth
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
