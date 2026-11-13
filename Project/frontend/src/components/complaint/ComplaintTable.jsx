import React from 'react';
import { Link } from 'react-router-dom';
import { AlertTriangle } from 'lucide-react';
import { StatusBadge, PriorityBadge } from './StatusBadge';
import { formatDateTime, formatRelativeDue } from '../../utils/formatDate';

export default function ComplaintTable({ complaints, emptyMessage = 'No complaints found.' }) {
  if (!complaints || complaints.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500">
        {emptyMessage}
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
        <thead className="bg-slate-50 text-xs font-semibold uppercase tracking-wide text-slate-600">
          <tr>
            <th className="px-4 py-3">Complaint #</th>
            <th className="px-4 py-3">Title &amp; Category</th>
            <th className="px-4 py-3">Priority</th>
            <th className="px-4 py-3">Status</th>
            <th className="px-4 py-3">Department / Assignee</th>
            <th className="px-4 py-3">SLA Target</th>
            <th className="px-4 py-3">Created</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {complaints.map((c) => (
            <tr key={c.id} className="hover:bg-slate-50/70 transition-colors">
              <td className="whitespace-nowrap px-4 py-3 font-mono text-xs font-semibold text-brand-700">
                <Link to={`/complaints/${c.id}`} className="hover:underline">
                  {c.complaintNumber}
                </Link>
                {c.escalated && (
                  <span
                    title="SLA Escalated"
                    className="ml-1.5 inline-flex items-center gap-0.5 rounded bg-rose-100 px-1.5 py-0.5 text-[10px] font-semibold text-rose-700"
                  >
                    <AlertTriangle className="h-3 w-3" /> SLA
                  </span>
                )}
              </td>
              <td className="px-4 py-3">
                <Link to={`/complaints/${c.id}`} className="font-medium text-slate-900 hover:text-brand-600">
                  {c.title}
                </Link>
                <div className="text-xs text-slate-500">
                  {c.categoryName} {c.location ? `• ${c.location}` : ''}
                </div>
              </td>
              <td className="whitespace-nowrap px-4 py-3">
                <PriorityBadge priority={c.priority} />
              </td>
              <td className="whitespace-nowrap px-4 py-3">
                <StatusBadge status={c.status} />
              </td>
              <td className="px-4 py-3 text-xs text-slate-600">
                <div className="font-medium text-slate-800">{c.departmentName || 'Unrouted'}</div>
                <div>{c.assignedEmployeeName ? `Assigned: ${c.assignedEmployeeName}` : 'Unassigned'}</div>
              </td>
              <td className="whitespace-nowrap px-4 py-3 text-xs text-slate-600">
                {formatRelativeDue(c.dueAt, c.status)}
              </td>
              <td className="whitespace-nowrap px-4 py-3 text-xs text-slate-500">
                {formatDateTime(c.createdAt)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

