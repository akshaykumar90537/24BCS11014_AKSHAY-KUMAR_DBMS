import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FilePlus2 } from 'lucide-react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import ComplaintTable from '../../components/complaint/ComplaintTable';
import complaintService from '../../services/complaintService';
import { extractErrorMessage } from '../../services/apiClient';
import { useAuth } from '../../context/AuthContext';

export default function Dashboard() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    complaintService
      .list({ page: 0, size: 10 })
      .then(setData)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  const list = data?.content || [];
  const total = data?.totalElements || 0;
  const openCount = list.filter((c) => !['CLOSED', 'REJECTED', 'RESOLVED'].includes(c.status)).length;
  const resolvedCount = list.filter((c) => ['RESOLVED', 'CLOSED'].includes(c.status)).length;
  const escalatedCount = list.filter((c) => c.escalated && !['CLOSED', 'REJECTED'].includes(c.status)).length;

  return (
    <PageShell
      title={`Welcome back, ${user?.name || ''}`}
      subtitle="Overview of your submitted complaints and recent status updates."
      actions={
        <Link to="/complaints/new">
          <Button>
            <FilePlus2 className="h-4 w-4" /> Submit Complaint
          </Button>
        </Link>
      }
    >
      {error && (
        <div className="mb-6 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { label: 'Total Submitted', value: total, tone: 'text-slate-900' },
          { label: 'Active / Open (Recent)', value: openCount, tone: 'text-brand-700' },
          { label: 'Resolved / Closed (Recent)', value: resolvedCount, tone: 'text-emerald-700' },
          { label: 'SLA Escalated (Recent)', value: escalatedCount, tone: 'text-rose-700' },
        ].map((card) => (
          <div key={card.label} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="text-xs font-medium text-slate-500">{card.label}</div>
            <div className={`mt-2 text-3xl font-bold ${card.tone}`}>{loading ? '—' : card.value}</div>
          </div>
        ))}
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-slate-900">Recent Complaints</h2>
          <Link to="/complaints" className="text-sm font-medium text-brand-600 hover:underline">
            View all
          </Link>
        </div>
        {loading ? (
          <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">
            Loading your complaints...
          </div>
        ) : (
          <ComplaintTable
            complaints={list}
            emptyMessage="You have not submitted any complaints yet. Click 'Submit Complaint' to file one."
          />
        )}
      </div>
    </PageShell>
  );
}

