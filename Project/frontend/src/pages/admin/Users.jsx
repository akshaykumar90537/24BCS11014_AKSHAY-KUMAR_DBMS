import React, { useCallback, useEffect, useState } from 'react';
import { UserPlus } from 'lucide-react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import Pagination from '../../components/common/Pagination';
import userService from '../../services/userService';
import departmentService from '../../services/departmentService';
import { extractErrorMessage } from '../../services/apiClient';
import { ROLE_LABELS } from '../../utils/statusLabels';

export default function Users() {
  const [data, setData] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('ROLE_EMPLOYEE');
  const [departmentId, setDepartmentId] = useState('');

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [usersRes, depts] = await Promise.all([
        userService.list({
          page,
          size: 15,
          search: search.trim() || undefined,
          role: roleFilter || undefined,
        }),
        departmentService.list(false),
      ]);
      setData(usersRes);
      setDepartments(depts);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [page, search, roleFilter]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const handleCreateUser = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await userService.create({
        name: name.trim(),
        email: email.trim(),
        password,
        role,
        departmentId: departmentId ? Number(departmentId) : null,
        memberType: role === 'ROLE_MANAGER' ? 'MANAGER' : 'EMPLOYEE',
      });
      setCreateOpen(false);
      setName('');
      setEmail('');
      setPassword('');
      loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleToggleActive = async (u) => {
    try {
      await userService.updateStatus(u.id, !u.active);
      loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleRoleChange = async (u, nextRole) => {
    try {
      await userService.updateRole(u.id, nextRole);
      loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <PageShell
      title="Users & Staff Management"
      subtitle="Create staff accounts, assign primary roles, and manage active status."
      actions={
        <Button onClick={() => setCreateOpen(true)}>
          <UserPlus className="h-4 w-4" /> Create Staff / User
        </Button>
      }
    >
      {error && (
        <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      <div className="mb-4 flex flex-wrap gap-3">
        <input
          type="text"
          value={search}
          onChange={(e) => {
            setPage(0);
            setSearch(e.target.value);
          }}
          placeholder="Search name or email..."
          className="rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-sm"
        />
        <select
          value={roleFilter}
          onChange={(e) => {
            setPage(0);
            setRoleFilter(e.target.value);
          }}
          className="rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-sm"
        >
          <option value="">All Roles</option>
          {Object.entries(ROLE_LABELS).map(([k, v]) => (
            <option key={k} value={k}>{v}</option>
          ))}
        </select>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">Loading users...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-600">
                <tr>
                  <th className="px-4 py-3">User</th>
                  <th className="px-4 py-3">Role</th>
                  <th className="px-4 py-3">Departments</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {(data?.content || []).map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900">{u.name}</div>
                      <div className="text-xs text-slate-500">{u.email}</div>
                    </td>
                    <td className="px-4 py-3">
                      <select
                        value={u.role}
                        onChange={(e) => handleRoleChange(u, e.target.value)}
                        className="rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-xs"
                      >
                        {Object.entries(ROLE_LABELS).map(([k, v]) => (
                          <option key={k} value={k}>{v}</option>
                        ))}
                      </select>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-600">
                      {u.departments?.length
                        ? u.departments.map((d) => `${d.departmentName} (${d.memberType})`).join(', ')
                        : '—'}
                    </td>
                    <td className="px-4 py-3 text-xs">
                      <span
                        className={`rounded-full px-2.5 py-0.5 font-medium ${
                          u.active ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'
                        }`}
                      >
                        {u.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Button
                        size="sm"
                        variant={u.active ? 'secondary' : 'success'}
                        onClick={() => handleToggleActive(u)}
                      >
                        {u.active ? 'Deactivate' : 'Activate'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {data && (
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPageChange={setPage}
          />
        )}
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Create Account">
        <form onSubmit={handleCreateUser} className="space-y-4">
          <div className="space-y-1">
            <label className="text-sm font-medium text-slate-700">Full Name</label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="space-y-1">
            <label className="text-sm font-medium text-slate-700">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="space-y-1">
            <label className="text-sm font-medium text-slate-700">Initial Password</label>
            <input
              type="password"
              required
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="space-y-1">
            <label className="text-sm font-medium text-slate-700">Role</label>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            >
              {Object.entries(ROLE_LABELS).map(([k, v]) => (
                <option key={k} value={k}>{v}</option>
              ))}
            </select>
          </div>
          {(role === 'ROLE_MANAGER' || role === 'ROLE_EMPLOYEE') && (
            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-700">Assign to Department (Optional)</label>
              <select
                value={departmentId}
                onChange={(e) => setDepartmentId(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="">Select department...</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>
          )}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setCreateOpen(false)}>Cancel</Button>
            <Button type="submit">Create Account</Button>
          </div>
        </form>
      </Modal>
    </PageShell>
  );
}

