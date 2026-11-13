import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Play, CheckCircle } from 'lucide-react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import Pagination from '../../components/common/Pagination';
import { StatusBadge, PriorityBadge } from '../../components/complaint/StatusBadge';
import complaintService from '../../services/complaintService';
import { extractErrorMessage } from '../../services/apiClient';
import { formatRelativeDue } from '../../utils/formatDate';

export default function EmployeeDashboard() {
  const [data, setData] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [resolveTarget, setResolveTarget] = useState(null);
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadQueue = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await complaintService.getEmployeeQueue({ page, size: 10 });
      setData(res);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    loadQueue();
  }, [loadQueue]);

  const handleStart = async (id) => {
    try {
      await complaintService.startWork(id);
      loadQueue();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleResolve = async (e) => {
    e.preventDefault();
    if (!resolutionNotes.trim()) return;
    setSubmitting(true);
    try {
      await complaintService.resolve(resolveTarget.id, resolutionNotes.trim());
      setResolveTarget(null);
      setResolutionNotes('');
      loadQueue();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const complaints = data?.content || [];

  return (
    <PageShell
      title="Employee Assigned Queue"
      subtitle="Investigate assigned complaints, update progress, and submit resolution details."
    >
      {error && (
        <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">Loading assigned complaints...</div>
        ) : complaints.length === 0 ? (
          <div className="p-10 text-center text-sm text-slate-500">
            No complaints are currently assigned to you.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-600">
                <tr>
                  <th className="px-4 py-3">Complaint #</th>
                  <th className="px-4 py-3">Title &amp; Location</th>
                  <th className="px-4 py-3">Priority</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Due Target</th>
                  <th className="px-4 py-3 text-right">Quick Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {complaints.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-mono text-xs font-semibold text-brand-700">
                      <Link to={`/complaints/${c.id}`} className="hover:underline">
                        {c.complaintNumber}
                      </Link>
                    </td>
                    <td className="px-4 py-3">
                      <Link to={`/complaints/${c.id}`} className="font-medium text-slate-900 hover:text-brand-600">
                        {c.title}
                      </Link>
                      <div className="text-xs text-slate-500">{c.categoryName} {c.location ? `• ${c.location}` : ''}</div>
                    </td>
                    <td className="px-4 py-3"><PriorityBadge priority={c.priority} /></td>
                    <td className="px-4 py-3"><StatusBadge status={c.status} /></td>
                    <td className="px-4 py-3 text-xs text-slate-600">{formatRelativeDue(c.dueAt, c.status)}</td>
                    <td className="px-4 py-3 text-right space-x-2 whitespace-nowrap">
                      {(c.status === 'ASSIGNED' || c.status === 'REOPENED') && (
                        <Button size="sm" onClick={() => handleStart(c.id)}>
                          <Play className="h-3.5 w-3.5" /> Start Work
                        </Button>
                      )}
                      {c.status === 'IN_PROGRESS' && (
                        <Button
                          size="sm"
                          variant="success"
                          onClick={() => {
                            setResolveTarget(c);
                            setResolutionNotes('');
                          }}
                        >
                          <CheckCircle className="h-3.5 w-3.5" /> Resolve
                        </Button>
                      )}
                      <Link to={`/complaints/${c.id}`}>
                        <Button size="sm" variant="secondary">Details</Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination
          page={data?.page || 0}
          totalPages={data?.totalPages || 0}
          totalElements={data?.totalElements || 0}
          onPageChange={setPage}
        />
      </div>

      <Modal
        open={Boolean(resolveTarget)}
        onClose={() => setResolveTarget(null)}
        title={`Resolve ${resolveTarget?.complaintNumber || ''}`}
      >
        <form onSubmit={handleResolve} className="space-y-4">
          <p className="text-sm text-slate-600">
            Provide resolution remarks explaining how the issue was addressed.
          </p>
          <textarea
            rows={4}
            required
            value={resolutionNotes}
            onChange={(e) => setResolutionNotes(e.target.value)}
            placeholder="Resolution notes..."
            className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
          />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setResolveTarget(null)}>
              Cancel
            </Button>
            <Button type="submit" variant="success" loading={submitting}>
              Submit Resolution
            </Button>
          </div>
        </form>
      </Modal>
    </PageShell>
  );
}

