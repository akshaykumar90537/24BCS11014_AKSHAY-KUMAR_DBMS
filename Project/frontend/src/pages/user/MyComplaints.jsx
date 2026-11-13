import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FilePlus2, Search } from 'lucide-react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import Pagination from '../../components/common/Pagination';
import ComplaintTable from '../../components/complaint/ComplaintTable';
import complaintService from '../../services/complaintService';
import departmentService from '../../services/departmentService';
import { extractErrorMessage } from '../../services/apiClient';

export default function MyComplaints() {
  const [data, setData] = useState(null);
  const [categories, setCategories] = useState([]);
  const [status, setStatus] = useState('');
  const [priority, setPriority] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    departmentService.listCategories().then(setCategories).catch(() => {});
  }, []);

  const loadComplaints = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await complaintService.list({
        page,
        size: 10,
        status: status || undefined,
        priority: priority || undefined,
        categoryId: categoryId || undefined,
        search: search.trim() || undefined,
      });
      setData(res);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [page, status, priority, categoryId, search]);

  useEffect(() => {
    loadComplaints();
  }, [loadComplaints]);

  return (
    <PageShell
      title="Complaints"
      subtitle="Search, filter, and inspect complaints visible to your role."
      actions={
        <Link to="/complaints/new">
          <Button>
            <FilePlus2 className="h-4 w-4" /> New Complaint
          </Button>
        </Link>
      }
    >
      <div className="mb-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => {
              setPage(0);
              setSearch(e.target.value);
            }}
            placeholder="Search # or title..."
            className="w-full rounded-xl border border-slate-300 pl-9 pr-3 py-2 text-sm focus:border-brand-500 focus:outline-none"
          />
        </div>

        <select
          value={status}
          onChange={(e) => {
            setPage(0);
            setStatus(e.target.value);
          }}
          className="rounded-xl border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none"
        >
          <option value="">All Statuses</option>
          {['SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED', 'REOPENED'].map((s) => (
            <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
          ))}
        </select>

        <select
          value={priority}
          onChange={(e) => {
            setPage(0);
            setPriority(e.target.value);
          }}
          className="rounded-xl border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none"
        >
          <option value="">All Priorities</option>
          {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((p) => (
            <option key={p} value={p}>{p}</option>
          ))}
        </select>

        <select
          value={categoryId}
          onChange={(e) => {
            setPage(0);
            setCategoryId(e.target.value);
          }}
          className="rounded-xl border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none"
        >
          <option value="">All Categories</option>
          {categories.map((cat) => (
            <option key={cat.id} value={cat.id}>{cat.name}</option>
          ))}
        </select>
      </div>

      {error && (
        <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      {loading ? (
        <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">
          Loading complaints...
        </div>
      ) : (
        <div className="rounded-xl border border-slate-200 bg-white overflow-hidden">
          <ComplaintTable complaints={data?.content || []} />
          <Pagination
            page={data?.page || 0}
            totalPages={data?.totalPages || 0}
            totalElements={data?.totalElements || 0}
            onPageChange={setPage}
          />
        </div>
      )}
    </PageShell>
  );
}

