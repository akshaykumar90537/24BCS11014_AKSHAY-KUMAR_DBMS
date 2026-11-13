export function formatDateTime(isoString) {
  if (!isoString) return '—';
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function formatRelativeDue(dueIso, status) {
  if (!dueIso) return 'No SLA target';
  if (status === 'CLOSED' || status === 'REJECTED' || status === 'RESOLVED') {
    return formatDateTime(dueIso);
  }
  const diffMs = new Date(dueIso).getTime() - Date.now();
  const absHours = Math.round(Math.abs(diffMs) / 3600000);
  if (diffMs < 0) {
    return `Overdue by ${absHours}h`;
  }
  if (absHours < 24) {
    return `Due in ${absHours}h`;
  }
  const days = Math.round(absHours / 24);
  return `Due in ${days}d`;
}

