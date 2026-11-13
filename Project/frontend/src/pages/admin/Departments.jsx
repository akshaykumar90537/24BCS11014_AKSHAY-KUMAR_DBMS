import React, { useCallback, useEffect, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import departmentService from '../../services/departmentService';
import userService from '../../services/userService';
import { extractErrorMessage } from '../../services/apiClient';

export default function Departments() {
  const [departments, setDepartments] = useState([]);
  const [staffUsers, setStaffUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [deptModal, setDeptModal] = useState(false);
  const [editingDept, setEditingDept] = useState(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [active, setActive] = useState(true);

  const [memberModalDept, setMemberModalDept] = useState(null);
  const [memberUserId, setMemberUserId] = useState('');
  const [memberType, setMemberType] = useState('EMPLOYEE');

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [depts, usersRes] = await Promise.all([
        departmentService.list(true),
        userService.list({ page: 0, size: 100 }),
      ]);
      setDepartments(depts);
      setStaffUsers(
        (usersRes.content || []).filter((u) => u.role === 'ROLE_MANAGER' || u.role === 'ROLE_EMPLOYEE')
      );
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const openCreate = () => {
    setEditingDept(null);
    setName('');
    setDescription('');
    setActive(true);
    setDeptModal(true);
  };

  const openEdit = (d) => {
    setEditingDept(d);
    setName(d.name);
    setDescription(d.description || '');
    setActive(d.active);
    setDeptModal(true);
  };

  const handleSaveDept = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (editingDept) {
        await departmentService.update(editingDept.id, { name, description, active });
      } else {
        await departmentService.create({ name, description });
      }
      setDeptModal(false);
      loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleAddMember = async (e) => {
    e.preventDefault();
    if (!memberUserId) return;
    setError('');
    try {
      await departmentService.addMember(memberModalDept.id, {
        userId: Number(memberUserId),
        memberType,
      });
      setMemberModalDept(null);
      loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleRemoveMember = async (deptId, userId) => {
    try {
      await departmentService.removeMember(deptId, userId);
      loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <PageShell
      title="Departments & Staff Memberships"
      subtitle="Manage organizational resolution departments, managers, and assigned employees."
      actions={
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" /> New Department
        </Button>
      }
    >
      {error && (
        <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      {loading ? (
        <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">
          Loading departments...
        </div>
      ) : (
        <div className="grid gap-5 md:grid-cols-2">
          {departments.map((d) => (
            <div key={d.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm space-y-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold text-slate-900">{d.name}</h3>
                    <span
                      className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                        d.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'
                      }`}
                    >
                      {d.active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-slate-600">{d.description || 'No description'}</p>
                </div>
                <div className="flex gap-2">
                  <Button size="sm" variant="secondary" onClick={() => openEdit(d)}>Edit</Button>
                  <Button
                    size="sm"
                    onClick={() => {
                      setMemberModalDept(d);
                      setMemberUserId(String(staffUsers[0]?.id || ''));
                      setMemberType('EMPLOYEE');
                    }}
                  >
                    + Member
                  </Button>
                </div>
              </div>

              <div className="border-t border-slate-100 pt-3">
                <div className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
                  Active Members ({d.members?.length || 0})
                </div>
                {d.members?.length ? (
                  <ul className="space-y-1.5">
                    {d.members.map((m) => (
                      <li
                        key={m.id}
                        className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-1.5 text-xs"
                      >
                        <div>
                          <span className="font-medium text-slate-900">{m.userName}</span>{' '}
                          <span className="text-slate-500">({m.memberType})</span>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleRemoveMember(d.id, m.userId)}
                          className="text-rose-600 hover:text-rose-800 p-1"
                          title="Deactivate membership"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-xs text-slate-400">No active members assigned.</p>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={deptModal}
        onClose={() => setDeptModal(false)}
        title={editingDept ? 'Update Department' : 'Create Department'}
      >
        <form onSubmit={handleSaveDept} className="space-y-4">
          <div className="space-y-1">
            <label className="text-sm font-medium text-slate-700">Name</label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="space-y-1">
            <label className="text-sm font-medium text-slate-700">Description</label>
            <textarea
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          {editingDept && (
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
              Active department
            </label>
          )}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setDeptModal(false)}>Cancel</Button>
            <Button type="submit">Save</Button>
          </div>
        </form>
      </Modal>

      <Modal
        open={Boolean(memberModalDept)}
        onClose={() => setMemberModalDept(null)}
        title={`Add Member to ${memberModalDept?.name || ''}`}
      >
        <form onSubmit={handleAddMember} className="space-y-4">
          <div className="space-y-1">
            <label className="text-sm font-medium text-slate-700">Staff Account</label>
            <select
              required
              value={memberUserId}
              onChange={(e) => setMemberUserId(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            >
              {staffUsers.map((u) => (
                <option key={u.id} value={u.id}>{u.name} ({u.email})</option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-sm font-medium text-slate-700">Membership Type</label>
            <select
              value={memberType}
              onChange={(e) => setMemberType(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            >
              <option value="EMPLOYEE">Employee</option>
              <option value="MANAGER">Manager</option>
            </select>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setMemberModalDept(null)}>Cancel</Button>
            <Button type="submit">Add Member</Button>
          </div>
        </form>
      </Modal>
    </PageShell>
  );
}

