import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Navbar from '../../components/layout/Navbar';
import Button from '../../components/common/Button';
import FormField from '../../components/forms/FormField';
import { useAuth } from '../../context/AuthContext';
import { extractErrorMessage } from '../../services/apiClient';
import { getDashboardPathForRole } from '../../utils/statusLabels';

const DEMO_ACCOUNTS = [
  { label: 'Admin', email: 'admin@resolveit.local', pass: 'Admin@1234' },
  { label: 'Manager', email: 'manager@resolveit.local', pass: 'Manager@1234' },
  { label: 'Employee', email: 'employee@resolveit.local', pass: 'Employee@1234' },
  { label: 'User', email: 'user@resolveit.local', pass: 'User@1234' },
];

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!email.trim() || !password) {
      setError('Email and password are required');
      return;
    }
    setSubmitting(true);
    try {
      const user = await login(email.trim(), password);
      navigate(getDashboardPathForRole(user.role), { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err, 'Invalid email or password'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />
      <div className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-md space-y-6 rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Sign in to ResolveIt</h1>
            <p className="mt-1 text-sm text-slate-600">Access your complaints, queues, or administration console.</p>
          </div>

          {error && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <FormField label="Email Address" required>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@organization.local"
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20"
              />
            </FormField>

            <FormField label="Password" required>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/20"
              />
            </FormField>

            <div className="flex items-center justify-between text-xs">
              <Link to="/forgot-password" className="font-medium text-brand-600 hover:underline">
                Forgot password?
              </Link>
              <Link to="/register" className="font-medium text-brand-600 hover:underline">
                Create an account
              </Link>
            </div>

            <Button type="submit" loading={submitting} className="w-full">
              Sign In
            </Button>
          </form>

          <div className="border-t border-slate-200 pt-4">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-400">
              Demo Credentials (Quick Fill)
            </p>
            <div className="grid grid-cols-2 gap-2">
              {DEMO_ACCOUNTS.map((acc) => (
                <button
                  key={acc.label}
                  type="button"
                  onClick={() => {
                    setEmail(acc.email);
                    setPassword(acc.pass);
                  }}
                  className="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-left text-xs hover:bg-slate-100"
                >
                  <div className="font-semibold text-slate-800">{acc.label}</div>
                  <div className="truncate text-[11px] text-slate-500">{acc.email}</div>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

