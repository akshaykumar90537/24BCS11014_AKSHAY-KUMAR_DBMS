import React, { useRef } from 'react';
import { UploadCloud } from 'lucide-react';

export default function FileUploader({ onSelect, disabled = false, accept = '.pdf,.png,.jpg,.jpeg,.webp,.txt,.csv,.doc,.docx' }) {
  const inputRef = useRef(null);

  const handleChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      onSelect(file);
      e.target.value = '';
    }
  };

  return (
    <div>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        onChange={handleChange}
        disabled={disabled}
        className="hidden"
      />
      <button
        type="button"
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
        className="flex w-full items-center justify-center gap-2 rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-600 transition hover:border-brand-400 hover:bg-brand-50/40 hover:text-brand-700 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <UploadCloud className="h-5 w-5" />
        <span>Click to upload evidence (PDF, PNG, JPG, TXT, CSV, DOC/DOCX • max 10 MB)</span>
      </button>
    </div>
  );
}

