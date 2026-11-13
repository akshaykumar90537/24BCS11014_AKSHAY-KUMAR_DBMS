import React from 'react';
import { PRIORITY_STYLES, STATUS_STYLES } from '../../utils/statusLabels';

export function StatusBadge({ status }) {
  const cfg = STATUS_STYLES[status] || {
    label: status,
    classes: 'bg-slate-100 text-slate-700 border-slate-200',
  };
  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${cfg.classes}`}>
      {cfg.label}
    </span>
  );
}

export function PriorityBadge({ priority }) {
  const cfg = PRIORITY_STYLES[priority] || {
    label: priority,
    classes: 'bg-slate-100 text-slate-700 border-slate-200',
  };
  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${cfg.classes}`}>
      {cfg.label}
    </span>
  );
}

export default StatusBadge;

