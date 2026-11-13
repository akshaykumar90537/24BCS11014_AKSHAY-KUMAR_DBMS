import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import Pagination from '../../components/common/Pagination';
import notificationService from '../../services/notificationService';
import { extractErrorMessage } from '../../services/apiClient';
import { formatDateTime } from '../../utils/formatDate';

export default function Notifications() {
  const [inbox, setInbox] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadInbox = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await notificationService.list(page, 15);
      setInbox(res);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    loadInbox();
  }, [loadInbox]);

  const handleMarkRead = async (id) => {
    await notificationService.markRead(id);
    loadInbox();
  };

  const handleMarkAllRead = async () => {
    await notificationService.markAllRead();
    loadInbox();
  };

  const items = inbox?.page?.content || [];

  return (
    <PageShell
      title="Notifications"
      subtitle={`You have ${inbox?.unreadCount ?? 0} unread notification(s).`}
      actions={
        <Button variant="secondary" size="sm" onClick={handleMarkAllRead} disabled={!inbox?.unreadCount}>
          <CheckCheck className="h-4 w-4" /> Mark All Read
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
          <div className="p-8 text-center text-sm text-slate-500">Loading notifications...</div>
        ) : items.length === 0 ? (
          <div className="p-10 text-center text-sm text-slate-500">No notifications yet.</div>
        ) : (
          <ul className="divide-y divide-slate-100">
            {items.map((n) => (
              <li
                key={n.id}
                className={`flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 px-5 py-4 ${
                  n.readAt ? 'bg-white' : 'bg-brand-50/40'
                }`}
              >
                <div className="flex items-start gap-3">
                  <div className={`mt-0.5 rounded-lg p-2 ${n.readAt ? 'bg-slate-100 text-slate-400' : 'bg-brand-100 text-brand-700'}`}>
                    <Bell className="h-4 w-4" />
                  </div>
                  <div>
                    <div className="text-sm font-medium text-slate-900">{n.message}</div>
                    <div className="mt-1 flex flex-wrap items-center gap-3 text-xs text-slate-500">
                      <span>{n.type.replace(/_/g, ' ')}</span>
                      <span>• {formatDateTime(n.createdAt)}</span>
                      {n.complaintId && (
                        <Link to={`/complaints/${n.complaintId}`} className="font-mono font-medium text-brand-600 hover:underline">
                          View {n.complaintNumber}
                        </Link>
                      )}
                    </div>
                  </div>
                </div>
                {!n.readAt && (
                  <Button size="sm" variant="secondary" onClick={() => handleMarkRead(n.id)}>
                    Mark Read
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
        {inbox?.page && (
          <Pagination
            page={inbox.page.page}
            totalPages={inbox.page.totalPages}
            totalElements={inbox.page.totalElements}
            onPageChange={setPage}
          />
        )}
      </div>
    </PageShell>
  );
}

