import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AlertTriangle, Search } from 'lucide-react';
import Navbar from '../../components/layout/Navbar';
import Button from '../../components/common/Button';
import { StatusBadge, PriorityBadge } from '../../components/complaint/StatusBadge';
import complaintService from '../../services/complaintService';
import { extractErrorMessage } from '../../services/apiClient';
import { formatDateTime } from '../../utils/formatDate';

export default function Track() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initial = searchParams.get('number') || '';
  const [complaintNumber, setComplaintNumber] = useState(initial);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const performTrack = async (num) => {
    if (!num?.trim()) return;
    setLoading(true);
    setError('');
    setResult(null);
    try {
      const data = await complaintService.trackPublic(num.trim());
      setResult(data);
    } catch (err) {
      setError(extractErrorMessage(err, 'Complaint not found'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (initial) {
      performTrack(initial);
    }
  }, [initial]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!complaintNumber.trim()) return;
    setSearchParams({ number: complaintNumber.trim() });
    performTrack(complaintNumber.trim());
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />
      <main className="max-w-3xl w-full mx-auto px-4 py-12 space-y-6">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-4">
          <h1 className="text-2xl font-bold text-slate-900">Public Complaint Tracker</h1>
          <p className="text-sm text-slate-600">
            Enter a complaint tracking number to inspect non-sensitive status milestones. Sign in to view full descriptions, attachments, and comments.
          </p>
          <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
              <input
                type="text"
                value={complaintNumber}
                onChange={(e) => setComplaintNumber(e.target.value)}
                placeholder="e.g. CMP-2026-000001"
                className="w-full rounded-xl border border-slate-300 pl-10 pr-4 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
              />
            </div>
            <Button type="submit" loading={loading}>
              Track
            </Button>
          </form>
        </div>

        {error && (
          <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {error}
          </div>
        )}

        {result && (
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-4">
              <div>
                <div className="font-mono text-lg font-bold text-brand-700">{result.complaintNumber}</div>
                <div className="text-xs text-slate-500">
                  Category: {result.categoryName || '—'} • Department: {result.departmentName || 'Pending routing'}
                </div>
              </div>
              <div className="flex items-center gap-2">
                <PriorityBadge priority={result.priority} />
                <StatusBadge status={result.status} />
                {result.escalated && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-0.5 text-xs font-semibold text-rose-700">
                    <AlertTriangle className="h-3.5 w-3.5" /> Escalated
                  </span>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm">
              <div className="rounded-xl bg-slate-50 p-3">
                <div className="text-xs text-slate-500">Submitted</div>
                <div className="font-medium text-slate-900">{formatDateTime(result.createdAt)}</div>
              </div>
              <div className="rounded-xl bg-slate-50 p-3">
                <div className="text-xs text-slate-500">SLA Target</div>
                <div className="font-medium text-slate-900">{formatDateTime(result.dueAt)}</div>
              </div>
              <div className="rounded-xl bg-slate-50 p-3">
                <div className="text-xs text-slate-500">Resolved</div>
                <div className="font-medium text-slate-900">{formatDateTime(result.resolvedAt)}</div>
              </div>
            </div>

            <div>
              <h2 className="text-sm font-semibold text-slate-900 mb-3">Status Milestones</h2>
              <ol className="space-y-3 border-l-2 border-slate-200 pl-4">
                {result.milestones?.map((m, idx) => (
                  <li key={idx} className="relative">
                    <span className="absolute -left-[21px] top-1.5 h-2.5 w-2.5 rounded-full bg-brand-600 ring-4 ring-white" />
                    <div className="flex flex-wrap items-center gap-2 text-sm">
                      <span className="font-medium text-slate-900">{m.eventType.replace(/_/g, ' ')}</span>
                      {m.status && <StatusBadge status={m.status} />}
                    </div>
                    <div className="text-xs text-slate-500">{formatDateTime(m.timestamp)}</div>
                  </li>
                ))}
              </ol>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

