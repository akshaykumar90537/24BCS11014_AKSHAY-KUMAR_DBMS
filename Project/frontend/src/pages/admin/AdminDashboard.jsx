import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import Pagination from '../../components/common/Pagination';
import { StatusBadge, PriorityBadge } from '../../components/complaint/StatusBadge';
import reportService from '../../services/reportService';
import complaintService from '../../services/complaintService';
import departmentService from '../../services/departmentService';
import { extractErrorMessage } from '../../services/apiClient';
import { formatDateTime } from '../../utils/formatDate';

export default function AdminDashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [complaintsPage, setComplaintsPage] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [page, setPage] = useState(0);
  const [filterStatus, setFilterStatus] = useState('');
  const [escalatedOnly, setEscalatedOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [routeTarget, setRouteTarget] = useState(null);
  const [selectedDeptId, setSelectedDeptId] = useState('');
  const [routeReason, setRouteReason] = useState('');
  const [routing, setRouting] = useState(false);

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [dash, list, depts] = await Promise.all([
        reportService.getAdminDashboard(),
        complaintService.list({
          page,
          size: 10,
          status: filterStatus || undefined,
          escalated: escalatedOnly ? true : undefined,
        }),
        departmentService.list(false),
      ]);
      setDashboard(dash);
      setComplaintsPage(list);
      setDepartments(depts);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [page, filterStatus, escalatedOnly]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const handleRouteSubmit = async (e) => {
    e.preventDefault();
    if (!selectedDeptId) return;
    setRouting(true);
    try {
      await complaintService.routeToDepartment(routeTarget.id, Number(selectedDeptId), routeReason.trim() || null);
      setRouteTarget(null);
      loadAll();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setRouting(false);
    }
  };

  const s = dashboard?.summary;

  return (
    <PageShell
      title="Administrator Dashboard"
      subtitle="Organization-wide complaint metrics, SLA compliance, and routing queue."
      actions={
        <Link to="/reports">
          <Button variant="secondary">Full Reports &amp; CSV Export</Button>
        </Link>
      }
    >
      {error && (
        <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      {/* KPI cards */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {[
          { label: 'Total Complaints', value: s?.totalComplaints ?? '—', tone: 'text-slate-900' },
          { label: 'Open Active', value: s?.openComplaints ?? '—', tone: 'text-brand-700' },
          { label: 'Escalated Active', value: s?.escalatedActive ?? '—', tone: 'text-rose-700' },
          {
            label: 'SLA Compliance',
            value: s?.slaCompliancePercent != null ? `${s.slaCompliancePercent}%` : '—',
            tone: 'text-emerald-700',
          },
          {
            label: 'Avg Resolution',
            value: s?.averageResolutionHours != null ? `${s.averageResolutionHours}h` : '—',
            tone: 'text-indigo-700',
          },
        ].map((k) => (
          <div key={k.label} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="text-xs font-medium text-slate-500">{k.label}</div>
            <div className={`mt-2 text-2xl font-bold ${k.tone}`}>{loading ? '—' : k.value}</div>
          </div>
        ))}
      </div>

      {/* Charts */}
      <div className="mb-6 grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-slate-900">Department Volume (Open vs Resolved)</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={dashboard?.departmentMetrics || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="departmentName" tick={{ fontSize: 11 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Legend />
                <Bar dataKey="openCount" name="Open" fill="#2563eb" radius={[4, 4, 0, 0]} />
                <Bar dataKey="resolvedCount" name="Resolved" fill="#10b981" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-slate-900">Monthly Submission vs Resolution Trend</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={dashboard?.monthlyTrend || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="period" tick={{ fontSize: 11 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="submittedCount" name="Submitted" stroke="#2563eb" strokeWidth={2} />
                <Line type="monotone" dataKey="resolvedCount" name="Resolved" stroke="#10b981" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Global Queue */}
      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-5 py-4">
          <h2 className="text-base font-semibold text-slate-900">Global Complaint Queue</h2>
          <div className="flex flex-wrap items-center gap-3">
            <label className="flex items-center gap-1.5 text-xs font-medium text-rose-700 cursor-pointer">
              <input
                type="checkbox"
                checked={escalatedOnly}
                onChange={(e) => {
                  setPage(0);
                  setEscalatedOnly(e.target.checked);
                }}
              />
              Escalated Only
            </label>
            <select
              value={filterStatus}
              onChange={(e) => {
                setPage(0);
                setFilterStatus(e.target.value);
              }}
              className="rounded-xl border border-slate-300 px-3 py-1.5 text-xs"
            >
              <option value="">All Statuses</option>
              {['SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED', 'REOPENED'].map((st) => (
                <option key={st} value={st}>{st.replace(/_/g, ' ')}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
            <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-600">
              <tr>
                <th className="px-4 py-3">Complaint #</th>
                <th className="px-4 py-3">Title &amp; Creator</th>
                <th className="px-4 py-3">Priority</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Department</th>
                <th className="px-4 py-3">Created</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {(complaintsPage?.content || []).map((c) => (
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
                    <div className="text-xs text-slate-500">{c.creatorName} • {c.categoryName}</div>
                  </td>
                  <td className="px-4 py-3"><PriorityBadge priority={c.priority} /></td>
                  <td className="px-4 py-3"><StatusBadge status={c.status} /></td>
                  <td className="px-4 py-3 text-xs text-slate-700">{c.departmentName || 'Unrouted'}</td>
                  <td className="px-4 py-3 text-xs text-slate-500">{formatDateTime(c.createdAt)}</td>
                  <td className="px-4 py-3 text-right space-x-2 whitespace-nowrap">
                    {!['CLOSED', 'REJECTED'].includes(c.status) && (
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => {
                          setRouteTarget(c);
                          setSelectedDeptId(String(c.departmentId || departments[0]?.id || ''));
                          setRouteReason('');
                        }}
                      >
                        Route
                      </Button>
                    )}
                    <Link to={`/complaints/${c.id}`}>
                      <Button size="sm">Inspect</Button>
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {complaintsPage && (
          <Pagination
            page={complaintsPage.page}
            totalPages={complaintsPage.totalPages}
            totalElements={complaintsPage.totalElements}
            onPageChange={setPage}
          />
        )}
      </div>

      <Modal
        open={Boolean(routeTarget)}
        onClose={() => setRouteTarget(null)}
        title={`Route ${routeTarget?.complaintNumber || ''}`}
      >
        <form onSubmit={handleRouteSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="block text-sm font-medium text-slate-700">Department</label>
            <select
              required
              value={selectedDeptId}
              onChange={(e) => setSelectedDeptId(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            >
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </div>
          <div className="space-y-1.5">
            <label className="block text-sm font-medium text-slate-700">Routing Notes (Optional)</label>
            <textarea
              rows={3}
              value={routeReason}
              onChange={(e) => setRouteReason(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setRouteTarget(null)}>Cancel</Button>
            <Button type="submit" loading={routing}>Route Complaint</Button>
          </div>
        </form>
      </Modal>
    </PageShell>
  );
}

