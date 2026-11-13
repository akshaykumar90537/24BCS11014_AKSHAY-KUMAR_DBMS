import React, { useCallback, useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import departmentService from '../../services/departmentService';
import { extractErrorMessage } from '../../services/apiClient';

export default function Categories() {
  const [categories, setCategories] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [defaultDepartmentId, setDefaultDepartmentId] = useState('');
  const [active, setActive] = useState(true);

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [cats, depts] = await Promise.all([
        departmentService.listCategories(true),
        departmentService.list(false),
      ]);
      setCategories(cats);
      setDepartments(depts);
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
    setEditing(null);
    setName('');
    setDescription('');
    setDefaultDepartmentId('');
    setActive(true);
    setModalOpen(true);
  };

  const openEdit = (cat) => {
    setEditing(cat);
    setName(cat.name);
    setDescription(cat.description || '');
    setDefaultDepartmentId(cat.defaultDepartmentId ? String(cat.defaultDepartmentId) : '');
    setActive(cat.active);
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const payload = {
        name: name.trim(),
        description: description.trim() || null,
        defaultDepartmentId: defaultDepartmentId ? Number(defaultDepartmentId) : null,
        active,
      };
      if (editing) {
        await departmentService.updateCategory(editing.id, payload);
      } else {
        await departmentService.createCategory(payload);
      }
      setModalOpen(false);
      loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <PageShell
      title="Complaint Categories"
      subtitle="Configure complaint classifications and optional default department routing."
      actions={
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" /> New Category
        </Button>
      }
    >
      {error && (
        <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">Loading categories...</div>
        ) : (
          <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-600">
              <tr>
                <th className="px-4 py-3">Category</th>
                <th className="px-4 py-3">Default Department</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {categories.map((cat) => (
                <tr key={cat.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3">
                    <div className="font-medium text-slate-900">{cat.name}</div>
                    <div className="text-xs text-slate-500">{cat.description}</div>
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-700">
                    {cat.defaultDepartmentName || 'Manual routing'}
                  </td>
                  <td className="px-4 py-3 text-xs">
                    <span
                      className={`rounded-full px-2.5 py-0.5 font-medium ${
                        cat.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'
                      }`}
                    >
                      {cat.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Button size="sm" variant="secondary" onClick={() => openEdit(cat)}>Edit</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Update Category' : 'New Category'}>
        <form onSubmit={handleSave} className="space-y-4">
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
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="space-y-1">
            <label className="text-sm font-medium text-slate-700">Default Department (Optional)</label>
            <select
              value={defaultDepartmentId}
              onChange={(e) => setDefaultDepartmentId(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            >
              <option value="">None (Manual Admin Routing)</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </div>
          {editing && (
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
              Active category
            </label>
          )}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setModalOpen(false)}>Cancel</Button>
            <Button type="submit">Save</Button>
          </div>
        </form>
      </Modal>
    </PageShell>
  );
}

