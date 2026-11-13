import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Landing from '../pages/public/Landing';
import Login from '../pages/public/Login';
import Register from '../pages/public/Register';
import Track from '../pages/public/Track';
import ForgotPassword from '../pages/public/ForgotPassword';
import ResetPassword from '../pages/public/ResetPassword';
import Unauthorized from '../pages/public/Unauthorized';
import Dashboard from '../pages/user/Dashboard';
import SubmitComplaint from '../pages/user/SubmitComplaint';
import MyComplaints from '../pages/user/MyComplaints';
import EmployeeDashboard from '../pages/staff/EmployeeDashboard';
import ManagerDashboard from '../pages/staff/ManagerDashboard';
import AdminDashboard from '../pages/admin/AdminDashboard';
import Departments from '../pages/admin/Departments';
import Categories from '../pages/admin/Categories';
import Users from '../pages/admin/Users';
import Reports from '../pages/admin/Reports';
import ComplaintDetails from '../pages/shared/ComplaintDetails';
import Notifications from '../pages/shared/Notifications';
import Profile from '../pages/shared/Profile';

function ProtectedRoute({ allowedRoles, children }) {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 text-sm text-slate-500">
        Loading session...
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />;
  }
  return children;
}

export default function AppRouter() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/track" element={<Track />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/unauthorized" element={<Unauthorized />} />

      {/* User & Shared Protected routes */}
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/complaints"
        element={
          <ProtectedRoute>
            <MyComplaints />
          </ProtectedRoute>
        }
      />
      <Route
        path="/complaints/new"
        element={
          <ProtectedRoute>
            <SubmitComplaint />
          </ProtectedRoute>
        }
      />
      <Route
        path="/complaints/:id"
        element={
          <ProtectedRoute>
            <ComplaintDetails />
          </ProtectedRoute>
        }
      />
      <Route
        path="/notifications"
        element={
          <ProtectedRoute>
            <Notifications />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <Profile />
          </ProtectedRoute>
        }
      />

      {/* Staff routes */}
      <Route
        path="/employee"
        element={
          <ProtectedRoute allowedRoles={['ROLE_EMPLOYEE', 'ROLE_ADMIN']}>
            <EmployeeDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/manager"
        element={
          <ProtectedRoute allowedRoles={['ROLE_MANAGER', 'ROLE_ADMIN']}>
            <ManagerDashboard />
          </ProtectedRoute>
        }
      />

      {/* Admin & Reports routes */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <AdminDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/departments"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <Departments />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/categories"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <Categories />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/users"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
            <Users />
          </ProtectedRoute>
        }
      />
      <Route
        path="/reports"
        element={
          <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_MANAGER']}>
            <Reports />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

