import React, { useCallback, useEffect, useState } from 'react';
import { Download } from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import { useAuth } from '../../context/AuthContext';
import reportService from '../../services/reportService';
import departmentService from '../../services/departmentService';
import { extractErrorMessage } from '../../services/apiClient';

export default function Reports() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN';

  const [departments, setDepartments] = useState([]);
  const [departmentId, setDepartmentId] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [summary, setSummary] = useState(null);
  const [resolutionMetrics, setResolutionMetrics] = useState([]);
  const [slaPolicies, setSlaPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const buildParams = useCallback(() => {
    const params = {};
    if (departmentId) params.departmentId = Number(departmentId);
    if (fromDate) params.from = new Date(`${fromDate}T00:00:00Z`).toISOString();
    if (toDate) params.to = new Date(`${toDate}T23:59:59Z`).toISOString();
    return params;
  }, [departmentId, fromDate, toDate]);

  const loadReports = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = buildParams();
      const [sum, resTime] = await Promise.all([
        reportService.getSummary(params),
        reportService.getResolutionTime({ ...params, groupBy: 'category' }),
      ]);
      setSummary(sum);
      setResolutionMetrics(resTime);
      if (isAdmin) {
        const policies = await reportService.listSlaPolicies();
        setSlaPolicies(policies);
      }
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [buildParams, isAdmin]);

  useEffect(() => {
    departmentService.list(false).then(setDepartments).catch(() => {});
  }, []);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  const handleUpdateSla = async (policy, targetHours, reopenWindowDays) => {
    try {
      await reportService.updateSlaPolicy(policy.priority, {
        targetHours: Number(targetHours),
        reopenWindowDays: Number(reopenWindowDays),
      });
      loadReports();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const statusChartData = summary?.byStatus
    ? Object.entries(summary.byStatus).map(([name, count]) => ({ name, count }))
    : [];

  return (
    <PageShell
      title="Reports & SLA Analytics"
      subtitle="Filter performance metrics by date range and department, or export CSV."
      actions={
        <Button onClick={() => reportService.exportCsv(buildParams())}>
          <Download className="h-4 w-4" /> Export CSV
        </Button>
      }
    >
      {error && (
        <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      {/* Filters */}
      <div className="mb-6 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm grid gap-3 sm:grid-cols-4">
        <input
          type="date"
          value={fromDate}
          onChange={(e) => setFromDate(e.target.value)}
          className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
        />
        <input
          type="date"
          value={toDate}
          onChange={(e) => setToDate(e.target.value)}
          className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
        />
        {isAdmin && (
          <select
            value={departmentId}
            onChange={(e) => setDepartmentId(e.target.value)}
            className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="">All Departments</option>
            {departments.map((d) => (
              <option key={d.id} value={d.id}>{d.name}</option>
            ))}
          </select>
        )}
        <Button variant="secondary" onClick={loadReports}>
          Apply Filters
        </Button>
      </div>

      {/* Summary Cards */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="text-xs text-slate-500">Total Complaints</div>
          <div className="mt-1 text-2xl font-bold text-slate-900">{loading ? '—' : summary?.totalComplaints}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="text-xs text-slate-500">Resolved / Closed</div>
          <div className="mt-1 text-2xl font-bold text-emerald-700">{loading ? '—' : summary?.resolvedOrClosed}</div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="text-xs text-slate-500">SLA Compliance</div>
          <div className="mt-1 text-2xl font-bold text-brand-700">
            {summary?.slaCompliancePercent != null ? `${summary.slaCompliancePercent}%` : '—'}
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="text-xs text-slate-500">Avg. Feedback Rating</div>
          <div className="mt-1 text-2xl font-bold text-amber-600">
            {summary?.averageFeedbackRating != null ? `${summary.averageFeedbackRating} / 5` : '—'}
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2 mb-6">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-slate-900">Breakdown by Status</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={statusChartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="name" tick={{ fontSize: 10 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="count" fill="#2563eb" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-slate-900">Resolution Time by Category</h2>
          {resolutionMetrics.length === 0 ? (
            <p className="text-sm text-slate-500">No resolved complaints in selected filter.</p>
          ) : (
            <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
              <thead className="bg-slate-50 font-semibold uppercase text-slate-600">
                <tr>
                  <th className="px-3 py-2">Category</th>
                  <th className="px-3 py-2">Resolved</th>
                  <th className="px-3 py-2">Avg (hrs)</th>
                  <th className="px-3 py-2">Median (hrs)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {resolutionMetrics.map((m) => (
                  <tr key={m.groupName}>
                    <td className="px-3 py-2 font-medium text-slate-900">{m.groupName}</td>
                    <td className="px-3 py-2">{m.resolvedCount}</td>
                    <td className="px-3 py-2">{m.averageHours}h</td>
                    <td className="px-3 py-2">{m.medianHours}h</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {isAdmin && slaPolicies.length > 0 && (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Configurable SLA &amp; Reopen Policies</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {slaPolicies.map((pol) => (
              <SlaPolicyCard key={pol.priority} policy={pol} onSave={handleUpdateSla} />
            ))}
          </div>
        </div>
      )}
    </PageShell>
  );
}

function SlaPolicyCard({ policy, onSave }) {
  const [hours, setHours] = useState(policy.targetHours);
  const [days, setDays] = useState(policy.reopenWindowDays);

  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 space-y-2.5 text-xs">
      <div className="font-bold text-slate-900">{policy.priority}</div>
      <div>
        <label className="text-slate-500">Target Hours</label>
        <input
          type="number"
          min={1}
          max={720}
          value={hours}
          onChange={(e) => setHours(e.target.value)}
          className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-sm"
        />
      </div>
      <div>
        <label className="text-slate-500">Reopen Window (Days)</label>
        <input
          type="number"
          min={0}
          max={90}
          value={days}
          onChange={(e) => setDays(e.target.value)}
          className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-sm"
        />
      </div>
      <Button size="sm" variant="secondary" className="w-full" onClick={() => onSave(policy, hours, days)}>
        Update Policy
      </Button>
    </div>
  );
}

