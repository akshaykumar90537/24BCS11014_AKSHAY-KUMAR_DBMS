import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle, UserPlus } from 'lucide-react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import Pagination from '../../components/common/Pagination';
import { StatusBadge, PriorityBadge } from '../../components/complaint/StatusBadge';
import complaintService from '../../services/complaintService';
import { extractErrorMessage } from '../../services/apiClient';
import { formatRelativeDue } from '../../utils/formatDate';

export default function ManagerDashboard() {
  const [queueData, setQueueData] = useState(null);
  const [overdueData, setOverdueData] = useState(null);
  const [tab, setTab] = useState('queue');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [assignModal, setAssignModal] = useState(null);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState('');
  const [assignReason, setAssignReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [queue, overdue] = await Promise.all([
        complaintService.getManagerQueue({ page, size: 10, status: statusFilter || undefined }),
        complaintService.getManagerOverdue({ page: 0, size: 20 }),
      ]);
      setQueueData(queue);
      setOverdueData(overdue);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const openAssign = (complaint) => {
    setAssignModal(complaint);
    const defaultEmp = queueData?.workload?.[0]?.employeeId || '';
    setSelectedEmployeeId(String(defaultEmp));
    setAssignReason('');
  };

  const handleAssignSubmit = async (e) => {
    e.preventDefault();
    if (!selectedEmployeeId) return;
    setSubmitting(true);
    try {
      if (assignModal.activeAssignmentId) {
        await complaintService.reassignEmployee(
          assignModal.id,
          assignModal.activeAssignmentId,
          Number(selectedEmployeeId),
          assignReason.trim() || 'Reassigned by department manager'
        );
      } else {
        await complaintService.assignEmployee(
          assignModal.id,
          Number(selectedEmployeeId),
          assignReason.trim() || 'Assigned by department manager'
        );
      }
      setAssignModal(null);
      loadData();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const activeList = tab === 'overdue' ? overdueData?.content || [] : queueData?.complaints?.content || [];

  return (
    <PageShell
      title="Department Manager Dashboard"
      subtitle="Monitor staff workload, assign complaints to team members, and handle overdue SLA alerts."
    >
      {error && (
        <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      {queueData?.workload && (
        <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {queueData.workload.map((w) => (
            <div key={w.employeeId} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="text-sm font-semibold text-slate-900">{w.employeeName}</div>
              <div className="text-xs text-slate-500">{w.employeeEmail}</div>
              <div className="mt-2 text-2xl font-bold text-brand-700">
                {w.activeCount} <span className="text-xs font-normal text-slate-500">active</span>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-2">
          <Button
            size="sm"
            variant={tab === 'queue' ? 'primary' : 'secondary'}
            onClick={() => setTab('queue')}
          >
            Department Queue ({queueData?.complaints?.totalElements || 0})
          </Button>
          <Button
            size="sm"
            variant={tab === 'overdue' ? 'danger' : 'secondary'}
            onClick={() => setTab('overdue')}
          >
            <AlertTriangle className="h-3.5 w-3.5" />
            Overdue / Escalated ({overdueData?.totalElements || 0})
          </Button>
        </div>

        {tab === 'queue' && (
          <select
            value={statusFilter}
            onChange={(e) => {
              setPage(0);
              setStatusFilter(e.target.value);
            }}
            className="rounded-xl border border-slate-300 bg-white px-3 py-1.5 text-sm"
          >
            <option value="">All Statuses</option>
            {['SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED'].map((s) => (
              <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
            ))}
          </select>
        )}
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">Loading department queue...</div>
        ) : activeList.length === 0 ? (
          <div className="p-10 text-center text-sm text-slate-500">No complaints in this view.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-600">
                <tr>
                  <th className="px-4 py-3">Complaint #</th>
                  <th className="px-4 py-3">Title</th>
                  <th className="px-4 py-3">Priority</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Assignee</th>
                  <th className="px-4 py-3">SLA</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {activeList.map((c) => (
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
                      <div className="text-xs text-slate-500">{c.categoryName}</div>
                    </td>
                    <td className="px-4 py-3"><PriorityBadge priority={c.priority} /></td>
                    <td className="px-4 py-3"><StatusBadge status={c.status} /></td>
                    <td className="px-4 py-3 text-xs text-slate-700">
                      {c.assignedEmployeeName || <span className="text-amber-600 font-medium">Unassigned</span>}
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-600">{formatRelativeDue(c.dueAt, c.status)}</td>
                    <td className="px-4 py-3 text-right space-x-2 whitespace-nowrap">
                      {!['CLOSED', 'REJECTED', 'RESOLVED'].includes(c.status) && (
                        <Button size="sm" onClick={() => openAssign(c)}>
                          <UserPlus className="h-3.5 w-3.5" />
                          {c.activeAssignmentId ? 'Reassign' : 'Assign'}
                        </Button>
                      )}
                      <Link to={`/complaints/${c.id}`}>
                        <Button size="sm" variant="secondary">Inspect</Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {tab === 'queue' && queueData?.complaints && (
          <Pagination
            page={queueData.complaints.page}
            totalPages={queueData.complaints.totalPages}
            totalElements={queueData.complaints.totalElements}
            onPageChange={setPage}
          />
        )}
      </div>

      <Modal
        open={Boolean(assignModal)}
        onClose={() => setAssignModal(null)}
        title={`${assignModal?.activeAssignmentId ? 'Reassign' : 'Assign'} ${assignModal?.complaintNumber || ''}`}
      >
        <form onSubmit={handleAssignSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="block text-sm font-medium text-slate-700">Department Staff Member</label>
            <select
              required
              value={selectedEmployeeId}
              onChange={(e) => setSelectedEmployeeId(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            >
              {queueData?.workload?.map((w) => (
                <option key={w.employeeId} value={w.employeeId}>
                  {w.employeeName} ({w.activeCount} active)
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1.5">
            <label className="block text-sm font-medium text-slate-700">
              Reason {assignModal?.activeAssignmentId ? '(Required for reassignment)' : '(Optional)'}
            </label>
            <textarea
              rows={3}
              required={Boolean(assignModal?.activeAssignmentId)}
              value={assignReason}
              onChange={(e) => setAssignReason(e.target.value)}
              placeholder="Assignment or reassignment notes..."
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setAssignModal(null)}>
              Cancel
            </Button>
            <Button type="submit" loading={submitting}>
              Save Assignment
            </Button>
          </div>
        </form>
      </Modal>
    </PageShell>
  );
}

