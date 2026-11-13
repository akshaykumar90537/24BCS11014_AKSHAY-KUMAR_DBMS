import React from 'react';
import Button from './Button';

export default function Pagination({ page, totalPages, totalElements, onPageChange }) {
  if (!totalPages || totalPages <= 1) return null;

  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
      <span>
        Page <strong className="text-slate-900">{page + 1}</strong> of{' '}
        <strong className="text-slate-900">{totalPages}</strong> ({totalElements} total)
      </span>
      <div className="flex items-center gap-2">
        <Button
          variant="secondary"
          size="sm"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </Button>
        <Button
          variant="secondary"
          size="sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </Button>
      </div>
    </div>
  );
}

