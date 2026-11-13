import React, { useState } from 'react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import FormField from '../../components/forms/FormField';
import { useAuth } from '../../context/AuthContext';
import authService from '../../services/authService';
import { extractErrorMessage } from '../../services/apiClient';
import { ROLE_LABELS } from '../../utils/statusLabels';

export default function Profile() {
  const { user, setUser } = useAuth();
  const [name, setName] = useState(user?.name || '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSaving(true);
    try {
      const updated = await authService.updateProfile({
        name: name.trim(),
        currentPassword: currentPassword || null,
        newPassword: newPassword || null,
      });
      setUser(updated);
      localStorage.setItem('resolveit_user', JSON.stringify(updated));
      setCurrentPassword('');
      setNewPassword('');
      setSuccess('Profile updated successfully.');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <PageShell title="My Profile" subtitle="Manage your display name and password.">
      <div className="max-w-xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-5">
        <div className="rounded-xl bg-slate-50 p-4 text-sm border border-slate-200 space-y-1">
          <div><span className="text-slate-500">Email:</span> <strong className="text-slate-900">{user?.email}</strong></div>
          <div><span className="text-slate-500">Role:</span> <strong className="text-slate-900">{ROLE_LABELS[user?.role] || user?.role}</strong></div>
          {user?.departments?.length > 0 && (
            <div>
              <span className="text-slate-500">Departments:</span>{' '}
              <strong className="text-slate-900">
                {user.departments.map((d) => `${d.departmentName} (${d.memberType})`).join(', ')}
              </strong>
            </div>
          )}
        </div>

        {success && (
          <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
            {success}
          </div>
        )}
        {error && (
          <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <FormField label="Full Name" required>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
            />
          </FormField>

          <hr className="border-slate-200" />
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
            Optional Password Change
          </p>

          <FormField label="Current Password">
            <input
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
            />
          </FormField>

          <FormField label="New Password" hint="Minimum 8 characters">
            <input
              type="password"
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
            />
          </FormField>

          <Button type="submit" loading={saving}>
            Save Changes
          </Button>
        </form>
      </div>
    </PageShell>
  );
}

