import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import authService from '../services/authService';
import { clearSession } from '../services/apiClient';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('resolveit_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(() => Boolean(localStorage.getItem('resolveit_access_token')));

  const refreshProfile = useCallback(async () => {
    const token = localStorage.getItem('resolveit_access_token');
    if (!token) {
      setUser(null);
      setLoading(false);
      return null;
    }
    try {
      const me = await authService.me();
      setUser(me);
      localStorage.setItem('resolveit_user', JSON.stringify(me));
      return me;
    } catch {
      clearSession();
      setUser(null);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshProfile();
  }, [refreshProfile]);

  const saveAuthSession = (authData) => {
    localStorage.setItem('resolveit_access_token', authData.accessToken);
    if (authData.refreshToken) {
      localStorage.setItem('resolveit_refresh_token', authData.refreshToken);
    }
    localStorage.setItem('resolveit_user', JSON.stringify(authData.user));
    setUser(authData.user);
    return authData.user;
  };

  const login = async (email, password) => {
    const data = await authService.login({ email, password });
    return saveAuthSession(data);
  };

  const register = async (name, email, password) => {
    const data = await authService.register({ name, email, password });
    return saveAuthSession(data);
  };

  const logout = async () => {
    const refreshToken = localStorage.getItem('resolveit_refresh_token');
    try {
      await authService.logout(refreshToken);
    } catch {
      // Clear local session regardless of server response
    } finally {
      clearSession();
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshProfile, setUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}

