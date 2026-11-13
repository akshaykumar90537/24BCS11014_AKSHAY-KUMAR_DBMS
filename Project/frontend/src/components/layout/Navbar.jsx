import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bell, LogOut, Menu, ShieldCheck, UserCircle } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import notificationService from '../../services/notificationService';
import { ROLE_LABELS } from '../../utils/statusLabels';

export default function Navbar({ onToggleSidebar }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    let active = true;
    if (!user) return;
    notificationService
      .list(0, 1)
      .then((res) => {
        if (active) setUnreadCount(res.unreadCount || 0);
      })
      .catch(() => {});
    const timer = setInterval(() => {
      notificationService
        .list(0, 1)
        .then((res) => {
          if (active) setUnreadCount(res.unreadCount || 0);
        })
        .catch(() => {});
    }, 30000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [user]);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-200 bg-white px-4 lg:px-6">
      <div className="flex items-center gap-3">
        {user && (
          <button
            type="button"
            onClick={onToggleSidebar}
            className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
            aria-label="Toggle menu"
          >
            <Menu className="h-5 w-5" />
          </button>
        )}
        <Link to="/" className="flex items-center gap-2 font-bold text-slate-900">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-white">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <span className="text-lg tracking-tight">ResolveIt</span>
        </Link>
      </div>

      <div className="flex items-center gap-3">
        {user ? (
          <>
            <Link
              to="/notifications"
              className="relative rounded-lg p-2 text-slate-600 hover:bg-slate-100"
              aria-label="Notifications"
            >
              <Bell className="h-5 w-5" />
              {unreadCount > 0 && (
                <span className="absolute right-1 top-1 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-rose-600 px-1 text-[10px] font-bold text-white">
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </Link>

            <Link
              to="/profile"
              className="hidden sm:flex items-center gap-2 rounded-lg px-2.5 py-1.5 text-sm hover:bg-slate-100"
            >
              <UserCircle className="h-5 w-5 text-slate-500" />
              <div className="text-left leading-tight">
                <div className="font-medium text-slate-800">{user.name}</div>
                <div className="text-[11px] text-slate-500">{ROLE_LABELS[user.role] || user.role}</div>
              </div>
            </Link>

            <button
              type="button"
              onClick={handleLogout}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-100"
            >
              <LogOut className="h-4 w-4" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </>
        ) : (
          <div className="flex items-center gap-2">
            <Link
              to="/track"
              className="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100"
            >
              Track Complaint
            </Link>
            <Link
              to="/login"
              className="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100"
            >
              Sign In
            </Link>
            <Link
              to="/register"
              className="rounded-lg bg-brand-600 px-3.5 py-1.5 text-sm font-medium text-white hover:bg-brand-700"
            >
              Register
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}

