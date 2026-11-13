import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  BarChart3,
  Bell,
  Building2,
  ClipboardList,
  FilePlus2,
  FolderTree,
  LayoutDashboard,
  Search,
  UserCheck,
  UserCog,
  Users,
  X,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export default function Sidebar({ open, onClose }) {
  const { user } = useAuth();
  if (!user) return null;

  const role = user.role;
  const links = [];

  if (role === 'ROLE_ADMIN') {
    links.push(
      { to: '/admin', label: 'Admin Dashboard', icon: LayoutDashboard, end: true },
      { to: '/admin/departments', label: 'Departments', icon: Building2 },
      { to: '/admin/categories', label: 'Categories', icon: FolderTree },
      { to: '/admin/users', label: 'Users & Staff', icon: Users },
      { to: '/reports', label: 'Reports & SLA', icon: BarChart3 },
    );
  } else if (role === 'ROLE_MANAGER') {
    links.push(
      { to: '/manager', label: 'Manager Queue', icon: LayoutDashboard, end: true },
      { to: '/reports', label: 'Dept. Reports', icon: BarChart3 },
    );
  } else if (role === 'ROLE_EMPLOYEE') {
    links.push({ to: '/employee', label: 'Assigned Queue', icon: UserCheck, end: true });
  } else {
    links.push({ to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, end: true });
  }

  links.push(
    { to: '/complaints/new', label: 'Submit Complaint', icon: FilePlus2 },
    { to: '/complaints', label: 'My / Visible Complaints', icon: ClipboardList, end: true },
    { to: '/track', label: 'Public Tracker', icon: Search },
    { to: '/notifications', label: 'Notifications', icon: Bell },
    { to: '/profile', label: 'Profile', icon: UserCog },
  );

  const navContent = (
    <nav className="flex h-full flex-col gap-1 p-4">
      <div className="mb-2 flex items-center justify-between lg:hidden">
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Navigation</span>
        <button type="button" onClick={onClose} className="rounded-lg p-1 text-slate-500 hover:bg-slate-100">
          <X className="h-5 w-5" />
        </button>
      </div>
      {links.map((item) => {
        const Icon = item.icon;
        return (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={onClose}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium transition ${
                isActive
                  ? 'bg-brand-50 text-brand-700'
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
              }`
            }
          >
            <Icon className="h-4 w-4 shrink-0" />
            <span>{item.label}</span>
          </NavLink>
        );
      })}
    </nav>
  );

  return (
    <>
      <aside className="hidden w-64 shrink-0 border-r border-slate-200 bg-white lg:block">
        {navContent}
      </aside>

      {open && (
        <div className="fixed inset-0 z-40 flex lg:hidden">
          <div className="fixed inset-0 bg-slate-900/40" onClick={onClose} />
          <aside className="relative z-50 w-64 bg-white shadow-xl">{navContent}</aside>
        </div>
      )}
    </>
  );
}

