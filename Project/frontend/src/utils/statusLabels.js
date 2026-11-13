export const STATUS_STYLES = {
  SUBMITTED: { label: 'Submitted', classes: 'bg-slate-100 text-slate-700 border-slate-300' },
  UNDER_REVIEW: { label: 'Under Review', classes: 'bg-amber-50 text-amber-800 border-amber-200' },
  ASSIGNED: { label: 'Assigned', classes: 'bg-sky-50 text-sky-800 border-sky-200' },
  IN_PROGRESS: { label: 'In Progress', classes: 'bg-indigo-50 text-indigo-800 border-indigo-200' },
  RESOLVED: { label: 'Resolved', classes: 'bg-emerald-50 text-emerald-800 border-emerald-200' },
  CLOSED: { label: 'Closed', classes: 'bg-zinc-100 text-zinc-700 border-zinc-300' },
  REJECTED: { label: 'Rejected', classes: 'bg-rose-50 text-rose-800 border-rose-200' },
  REOPENED: { label: 'Reopened', classes: 'bg-orange-50 text-orange-800 border-orange-200' },
};

export const PRIORITY_STYLES = {
  CRITICAL: { label: 'Critical', classes: 'bg-red-100 text-red-800 border-red-200' },
  HIGH: { label: 'High', classes: 'bg-orange-100 text-orange-800 border-orange-200' },
  MEDIUM: { label: 'Medium', classes: 'bg-blue-50 text-blue-700 border-blue-200' },
  LOW: { label: 'Low', classes: 'bg-slate-100 text-slate-600 border-slate-200' },
};

export const ROLE_LABELS = {
  ROLE_USER: 'Complainant',
  ROLE_ADMIN: 'Administrator',
  ROLE_MANAGER: 'Dept. Manager',
  ROLE_EMPLOYEE: 'Support Staff',
};

export function getDashboardPathForRole(role) {
  switch (role) {
    case 'ROLE_ADMIN':
      return '/admin';
    case 'ROLE_MANAGER':
      return '/manager';
    case 'ROLE_EMPLOYEE':
      return '/employee';
    default:
      return '/dashboard';
  }
}

