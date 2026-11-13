import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle } from 'lucide-react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import FormField from '../../components/forms/FormField';
import FileUploader from '../../components/forms/FileUploader';
import departmentService from '../../services/departmentService';
import complaintService from '../../services/complaintService';
import { extractErrorMessage } from '../../services/apiClient';

export default function SubmitComplaint() {
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [duplicates, setDuplicates] = useState([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [location, setLocation] = useState('');
  const [files, setFiles] = useState([]);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    departmentService
      .listCategories()
      .then((cats) => {
        setCategories(cats);
        if (cats.length > 0) setCategoryId(String(cats[0].id));
      })
      .catch((err) => setError(extractErrorMessage(err)));
  }, []);

  useEffect(() => {
    if (!categoryId) return;
    complaintService
      .potentialDuplicates(Number(categoryId))
      .then(setDuplicates)
      .catch(() => setDuplicates([]));
  }, [categoryId]);

  const handleAddFile = (file) => {
    if (files.length >= 5) {
      setError('Maximum 5 attachments allowed per complaint');
      return;
    }
    setFiles((prev) => [...prev, file]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!title.trim() || !description.trim() || !categoryId) {
      setError('Title, description, and category are required.');
      return;
    }

    setSubmitting(true);
    try {
      const created = await complaintService.create({
        title: title.trim(),
        description: description.trim(),
        categoryId: Number(categoryId),
        priority,
        location: location.trim() || null,
      });

      for (const file of files) {
        await complaintService.uploadAttachment(created.id, file);
      }

      navigate(`/complaints/${created.id}`);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to submit complaint'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PageShell
      title="Submit a New Complaint"
      subtitle="Provide clear details and optional supporting evidence for faster resolution."
    >
      <div className="grid gap-6 lg:grid-cols-3">
        <form
          onSubmit={handleSubmit}
          className="lg:col-span-2 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-5"
        >
          {error && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {error}
            </div>
          )}

          <FormField label="Complaint Title" required>
            <input
              type="text"
              maxLength={200}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Water leakage in Hostel Block B corridor"
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
            />
          </FormField>

          <div className="grid gap-4 sm:grid-cols-2">
            <FormField label="Category" required>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
              >
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </FormField>

            <FormField label="Priority">
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
              >
                <option value="LOW">Low (Routine)</option>
                <option value="MEDIUM">Medium (Standard)</option>
                <option value="HIGH">High (Urgent)</option>
                <option value="CRITICAL">Critical (Immediate)</option>
              </select>
            </FormField>
          </div>

          <FormField label="Location (Optional)">
            <input
              type="text"
              maxLength={255}
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="Building, room, or area"
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
            />
          </FormField>

          <FormField label="Description" required>
            <textarea
              rows={5}
              maxLength={10000}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe the issue, when it started, and any relevant context..."
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
            />
          </FormField>

          <div className="space-y-2">
            <label className="block text-sm font-medium text-slate-700">Evidence Attachments (Optional)</label>
            <FileUploader onSelect={handleAddFile} disabled={submitting} />
            {files.length > 0 && (
              <ul className="space-y-1.5 pt-1">
                {files.map((f, idx) => (
                  <li
                    key={`${f.name}-${idx}`}
                    className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-700 border border-slate-200"
                  >
                    <span className="truncate font-medium">
                      {f.name} ({Math.round(f.size / 1024)} KB)
                    </span>
                    <button
                      type="button"
                      onClick={() => setFiles((prev) => prev.filter((_, i) => i !== idx))}
                      className="text-rose-600 hover:underline ml-3"
                    >
                      Remove
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
              Cancel
            </Button>
            <Button type="submit" loading={submitting}>
              Submit Complaint
            </Button>
          </div>
        </form>

        <div className="space-y-4">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm space-y-3">
            <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
              <AlertCircle className="h-4 w-4 text-amber-500" />
              <span>Recent Open Issues in Category</span>
            </div>
            <p className="text-xs text-slate-500">
              Check if an active complaint already covers this issue before submitting a duplicate.
            </p>
            {duplicates.length === 0 ? (
              <div className="text-xs text-slate-400">No recent open complaints in this category.</div>
            ) : (
              <ul className="space-y-2">
                {duplicates.map((d) => (
                  <li key={d.id} className="rounded-xl bg-slate-50 p-3 text-xs border border-slate-200">
                    <div className="font-mono font-semibold text-brand-700">{d.complaintNumber}</div>
                    <div className="font-medium text-slate-900 mt-0.5">{d.title}</div>
                    {d.location && <div className="text-slate-500 mt-0.5">{d.location}</div>}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </PageShell>
  );
}

