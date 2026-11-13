import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../../components/layout/Navbar';
import Button from '../../components/common/Button';
import FormField from '../../components/forms/FormField';
import authService from '../../services/authService';
import { extractErrorMessage } from '../../services/apiClient';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const res = await authService.forgotPassword(email.trim());
      setMessage(res.message);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />
      <div className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-md space-y-5 rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
          <h1 className="text-xl font-bold text-slate-900">Forgot Password</h1>
          <p className="text-sm text-slate-600">Enter your email address to request a reset token.</p>
          {message && (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
              {message}
            </div>
          )}
          {error && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {error}
            </div>
          )}
          <form onSubmit={handleSubmit} className="space-y-4">
            <FormField label="Email Address" required>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
              />
            </FormField>
            <Button type="submit" loading={loading} className="w-full">
              Request Reset Token
            </Button>
          </form>
          <div className="flex justify-between text-xs">
            <Link to="/login" className="text-brand-600 hover:underline">Back to Sign In</Link>
            <Link to="/reset-password" className="text-brand-600 hover:underline">Have a reset token?</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

