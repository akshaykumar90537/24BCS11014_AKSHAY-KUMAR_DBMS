import React, { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  AlertTriangle,
  Download,
  Lock,
  MessageSquare,
  Paperclip,
  Star,
} from 'lucide-react';
import PageShell from '../../components/layout/PageShell';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import FileUploader from '../../components/forms/FileUploader';
import { StatusBadge, PriorityBadge } from '../../components/complaint/StatusBadge';
import { useAuth } from '../../context/AuthContext';
import complaintService from '../../services/complaintService';
import departmentService from '../../services/departmentService';
import { extractErrorMessage } from '../../services/apiClient';
import { formatDateTime, formatRelativeDue } from '../../utils/formatDate';

export default function ComplaintDetails() {
  const { id } = useParams();
  const { user } = useAuth();

  const [complaint, setComplaint] = useState(null);
  const [history, setHistory] = useState([]);
  const [comments, setComments] = useState([]);
  const [attachments, setAttachments] = useState([]);
  const [feedback, setFeedback] = useState(null);
  const [departments, setDepartments] = useState([]);
  const [deptMembers, setDeptMembers] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  // Comment state
  const [commentBody, setCommentBody] = useState('');
  const [visibility, setVisibility] = useState('PUBLIC');
  const [postingComment, setPostingComment] = useState(false);

  // Feedback state
  const [rating, setRating] = useState(5);
  const [fbComment, setFbComment] = useState('');
  const [submittingFeedback, setSubmittingFeedback] = useState(false);

  // Action modal state: 'route' | 'assign' | 'resolve' | 'reopen' | 'reject' | 'cancel'
  const [modalType, setModalType] = useState(null);
  const [modalInput, setModalInput] = useState('');
  const [selectedDeptId, setSelectedDeptId] = useState('');
  const [selectedEmployeeId, setSelectedEmployeeId] = useState('');
  const [modalLoading, setModalLoading] = useState(false);

  const isAdmin = user?.role === 'ROLE_ADMIN';
  const isManager = user?.role === 'ROLE_MANAGER';
  const isEmployee = user?.role === 'ROLE_EMPLOYEE';
  const isStaff = isAdmin || isManager || isEmployee;
  const isCreator = complaint?.creatorId === user?.id;
  const isAssignedToMe = complaint?.assignedEmployeeId === user?.id;

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [c, hist, comms, atts, fb] = await Promise.all([
        complaintService.getById(id),
        complaintService.getHistory(id),
        complaintService.getComments(id),
        complaintService.getAttachments(id),
        complaintService.getFeedback(id),
      ]);
      setComplaint(c);
      setHistory(hist);
      setComments(comms);
      setAttachments(atts);
      setFeedback(fb);

      if (isAdmin) {
        const depts = await departmentService.list(false);
        setDepartments(depts);
      }
      if ((isAdmin || isManager) && c.departmentId) {
        const members = await departmentService.listMembers(c.departmentId);
        setDeptMembers(members);
      }
    } catch (err) {
      setError(extractErrorMessage(err, 'Unable to load complaint'));
    } finally {
      setLoading(false);
    }
  }, [id, isAdmin, isManager]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const handleUpload = async (file) => {
    setActionError('');
    try {
      await complaintService.uploadAttachment(id, file);
      const updated = await complaintService.getAttachments(id);
      setAttachments(updated);
      const updatedHist = await complaintService.getHistory(id);
      setHistory(updatedHist);
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Attachment upload failed'));
    }
  };

  const handleAddComment = async (e) => {
    e.preventDefault();
    if (!commentBody.trim()) return;
    setPostingComment(true);
    setActionError('');
    try {
      await complaintService.addComment(id, commentBody.trim(), isStaff ? visibility : 'PUBLIC');
      setCommentBody('');
      const updated = await complaintService.getComments(id);
      setComments(updated);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setPostingComment(false);
    }
  };

  const handleFeedbackSubmit = async (e) => {
    e.preventDefault();
    setSubmittingFeedback(true);
    setActionError('');
    try {
      const created = await complaintService.submitFeedback(id, Number(rating), fbComment.trim() || null);
      setFeedback(created);
      const updatedHist = await complaintService.getHistory(id);
      setHistory(updatedHist);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setSubmittingFeedback(false);
    }
  };

  const handleSimpleAction = async (fn) => {
    setActionError('');
    try {
      await fn();
      loadAll();
    } catch (err) {
      setActionError(extractErrorMessage(err));
    }
  };

  const openModal = (type) => {
    setActionError('');
    setModalType(type);
    setModalInput('');
    if (type === 'route') {
      setSelectedDeptId(String(complaint?.departmentId || departments[0]?.id || ''));
    }
    if (type === 'assign') {
      setSelectedEmployeeId(String(deptMembers[0]?.userId || ''));
    }
  };

  const handleModalSubmit = async (e) => {
    e.preventDefault();
    setModalLoading(true);
    setActionError('');
    try {
      if (modalType === 'route') {
        await complaintService.routeToDepartment(id, Number(selectedDeptId), modalInput.trim() || null);
      } else if (modalType === 'assign') {
        if (complaint.activeAssignmentId) {
          await complaintService.reassignEmployee(
            id,
            complaint.activeAssignmentId,
            Number(selectedEmployeeId),
            modalInput.trim()
          );
        } else {
          await complaintService.assignEmployee(id, Number(selectedEmployeeId), modalInput.trim() || null);
        }
      } else if (modalType === 'resolve') {
        await complaintService.resolve(id, modalInput.trim());
      } else if (modalType === 'reopen') {
        await complaintService.reopen(id, modalInput.trim());
      } else if (modalType === 'reject') {
        await complaintService.reject(id, modalInput.trim());
      } else if (modalType === 'cancel') {
        await complaintService.cancel(id, modalInput.trim());
      }
      setModalType(null);
      await loadAll();
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setModalLoading(false);
    }
  };

  if (loading) {
    return (
      <PageShell title="Complaint Details">
        <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-sm text-slate-500">
          Loading complaint details...
        </div>
      </PageShell>
    );
  }

  if (error || !complaint) {
    return (
      <PageShell title="Complaint Details">
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-sm text-rose-700">
          {error || 'Complaint not found'}
        </div>
      </PageShell>
    );
  }

  const st = complaint.status;
  const canStart = (isAdmin || isAssignedToMe) && (st === 'ASSIGNED' || st === 'REOPENED');
  const canResolve = (isAdmin || isManager || isAssignedToMe) && st === 'IN_PROGRESS';
  const canClose = (isCreator || isAdmin || isManager) && st === 'RESOLVED';
  const canReopen = (isCreator && st === 'RESOLVED') || (isAdmin && ['RESOLVED', 'CLOSED', 'REJECTED'].includes(st));
  const canCancel = (isCreator || isAdmin) && ['SUBMITTED', 'UNDER_REVIEW'].includes(st);
  const canReject = (isAdmin || isManager) && ['SUBMITTED', 'UNDER_REVIEW'].includes(st);
  const canRoute = isAdmin && !['CLOSED', 'REJECTED'].includes(st);
  const canAssign = (isAdmin || isManager) && complaint.departmentId && !['CLOSED', 'REJECTED', 'RESOLVED'].includes(st);
  const canSubmitFeedback = isCreator && (st === 'RESOLVED' || st === 'CLOSED') && !feedback;

  return (
    <PageShell
      title={`${complaint.complaintNumber} — ${complaint.title}`}
      subtitle={`Submitted by ${complaint.creatorName} on ${formatDateTime(complaint.createdAt)}`}
      actions={
        <div className="flex flex-wrap gap-2">
          {canRoute && (
            <Button size="sm" variant="secondary" onClick={() => openModal('route')}>
              Route Department
            </Button>
          )}
          {canAssign && (
            <Button size="sm" onClick={() => openModal('assign')}>
              {complaint.activeAssignmentId ? 'Reassign Staff' : 'Assign Staff'}
            </Button>
          )}
          {canStart && (
            <Button size="sm" onClick={() => handleSimpleAction(() => complaintService.startWork(id))}>
              Start Investigation
            </Button>
          )}
          {canResolve && (
            <Button size="sm" variant="success" onClick={() => openModal('resolve')}>
              Mark Resolved
            </Button>
          )}
          {canClose && (
            <Button size="sm" variant="success" onClick={() => handleSimpleAction(() => complaintService.close(id))}>
              Accept &amp; Close
            </Button>
          )}
          {canReopen && (
            <Button size="sm" variant="secondary" onClick={() => openModal('reopen')}>
              Reopen
            </Button>
          )}
          {canCancel && (
            <Button size="sm" variant="danger" onClick={() => openModal('cancel')}>
              Cancel Complaint
            </Button>
          )}
          {canReject && (
            <Button size="sm" variant="danger" onClick={() => openModal('reject')}>
              Reject
            </Button>
          )}
        </div>
      }
    >
      {actionError && (
        <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          {actionError}
        </div>
      )}

      {complaint.escalated && (
        <div className="mb-5 flex items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
          <AlertTriangle className="h-5 w-5 shrink-0 text-rose-600" />
          <div>
            <strong>SLA Escalated:</strong> This complaint breached its SLA deadline ({formatDateTime(complaint.dueAt)}) and was flagged at {formatDateTime(complaint.escalatedAt)}.
          </div>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 space-y-6">
          {/* Core details */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge status={complaint.status} />
              <PriorityBadge priority={complaint.priority} />
              <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-700">
                {complaint.categoryName}
              </span>
            </div>

            <div className="whitespace-pre-wrap text-sm leading-relaxed text-slate-800">
              {complaint.description}
            </div>

            {complaint.resolutionNotes && (
              <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm">
                <div className="font-semibold text-emerald-900">Resolution Notes</div>
                <p className="mt-1 text-emerald-800 whitespace-pre-wrap">{complaint.resolutionNotes}</p>
              </div>
            )}
          </div>

          {/* Attachments */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-base font-semibold text-slate-900">
              <Paperclip className="h-4 w-4 text-slate-500" />
              <span>Evidence &amp; Attachments ({attachments.length})</span>
            </div>

            {attachments.length > 0 ? (
              <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200">
                {attachments.map((att) => (
                  <li key={att.id} className="flex items-center justify-between px-4 py-3 text-sm">
                    <div className="min-w-0">
                      <div className="truncate font-medium text-slate-900">{att.originalFilename}</div>
                      <div className="text-xs text-slate-500">
                        {Math.max(1, Math.round(att.sizeBytes / 1024))} KB • Uploaded by {att.uploaderName} on{' '}
                        {formatDateTime(att.createdAt)}
                      </div>
                    </div>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => complaintService.downloadAttachment(att.id, att.originalFilename)}
                    >
                      <Download className="h-3.5 w-3.5" /> Download
                    </Button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-slate-500">No attachments uploaded yet.</p>
            )}

            {!['CLOSED', 'REJECTED'].includes(complaint.status) && (
              <FileUploader onSelect={handleUpload} />
            )}
          </div>

          {/* Comments & Internal Notes */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-base font-semibold text-slate-900">
              <MessageSquare className="h-4 w-4 text-slate-500" />
              <span>Comments &amp; Updates ({comments.length})</span>
            </div>

            {comments.length === 0 ? (
              <p className="text-sm text-slate-500">No comments yet.</p>
            ) : (
              <div className="space-y-3">
                {comments.map((c) => {
                  const isInternal = c.visibility === 'INTERNAL';
                  return (
                    <div
                      key={c.id}
                      className={`rounded-xl border p-4 text-sm ${
                        isInternal
                          ? 'border-amber-200 bg-amber-50/70'
                          : 'border-slate-200 bg-slate-50/70'
                      }`}
                    >
                      <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                        <div className="flex items-center gap-2 font-semibold text-slate-900">
                          <span>{c.authorName}</span>
                          {isInternal && (
                            <span className="inline-flex items-center gap-1 rounded bg-amber-200/70 px-1.5 py-0.5 text-[10px] font-bold text-amber-900">
                              <Lock className="h-3 w-3" /> INTERNAL NOTE
                            </span>
                          )}
                        </div>
                        <span className="text-slate-500">{formatDateTime(c.createdAt)}</span>
                      </div>
                      <p className="mt-2 whitespace-pre-wrap text-slate-800">{c.body}</p>
                    </div>
                  );
                })}
              </div>
            )}

            <form onSubmit={handleAddComment} className="space-y-3 pt-2 border-t border-slate-100">
              <textarea
                rows={3}
                value={commentBody}
                onChange={(e) => setCommentBody(e.target.value)}
                placeholder="Write a comment or update..."
                className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none"
              />
              <div className="flex flex-wrap items-center justify-between gap-3">
                {isStaff ? (
                  <div className="flex items-center gap-3 text-xs">
                    <label className="flex items-center gap-1.5 cursor-pointer">
                      <input
                        type="radio"
                        name="visibility"
                        value="PUBLIC"
                        checked={visibility === 'PUBLIC'}
                        onChange={() => setVisibility('PUBLIC')}
                      />
                      <span>Public (Visible to complainant)</span>
                    </label>
                    <label className="flex items-center gap-1.5 cursor-pointer text-amber-800 font-medium">
                      <input
                        type="radio"
                        name="visibility"
                        value="INTERNAL"
                        checked={visibility === 'INTERNAL'}
                        onChange={() => setVisibility('INTERNAL')}
                      />
                      <span>Internal Note (Staff only)</span>
                    </label>
                  </div>
                ) : (
                  <span className="text-xs text-slate-500">Comments are visible to assigned staff.</span>
                )}
                <Button type="submit" size="sm" loading={postingComment}>
                  Post Comment
                </Button>
              </div>
            </form>
          </div>
        </div>

        {/* Right column: metadata, feedback & audit trail */}
        <div className="space-y-6">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm space-y-3 text-sm">
            <h2 className="font-semibold text-slate-900">Summary &amp; SLA</h2>
            <dl className="space-y-2.5 text-xs">
              <div className="flex justify-between gap-2">
                <dt className="text-slate-500">Department</dt>
                <dd className="font-medium text-slate-900">{complaint.departmentName || 'Unrouted'}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-slate-500">Assigned To</dt>
                <dd className="font-medium text-slate-900">{complaint.assignedEmployeeName || 'Unassigned'}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-slate-500">Location</dt>
                <dd className="font-medium text-slate-900">{complaint.location || '—'}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-slate-500">SLA Due</dt>
                <dd className="font-medium text-slate-900">{formatRelativeDue(complaint.dueAt, complaint.status)}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-slate-500">Resolved At</dt>
                <dd className="font-medium text-slate-900">{formatDateTime(complaint.resolvedAt)}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-slate-500">Closed At</dt>
                <dd className="font-medium text-slate-900">{formatDateTime(complaint.closedAt)}</dd>
              </div>
            </dl>
          </div>

          {(feedback || canSubmitFeedback) && (
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm space-y-3">
              <h2 className="font-semibold text-slate-900 flex items-center gap-2 text-sm">
                <Star className="h-4 w-4 text-amber-500" />
                <span>Resolution Feedback</span>
              </h2>
              {feedback ? (
                <div className="space-y-1.5 text-sm">
                  <div className="font-bold text-amber-600">
                    {'★'.repeat(feedback.rating)}{'☆'.repeat(5 - feedback.rating)} ({feedback.rating}/5)
                  </div>
                  {feedback.comment && <p className="text-slate-700 text-xs">{feedback.comment}</p>}
                  <div className="text-[11px] text-slate-400">Submitted {formatDateTime(feedback.createdAt)}</div>
                </div>
              ) : (
                <form onSubmit={handleFeedbackSubmit} className="space-y-3">
                  <div>
                    <label className="block text-xs font-medium text-slate-600 mb-1">Rating (1-5)</label>
                    <select
                      value={rating}
                      onChange={(e) => setRating(Number(e.target.value))}
                      className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
                    >
                      {[5, 4, 3, 2, 1].map((n) => (
                        <option key={n} value={n}>{n} Star{n > 1 ? 's' : ''}</option>
                      ))}
                    </select>
                  </div>
                  <textarea
                    rows={2}
                    value={fbComment}
                    onChange={(e) => setFbComment(e.target.value)}
                    placeholder="Optional feedback comment..."
                    className="w-full rounded-xl border border-slate-300 px-3 py-2 text-xs"
                  />
                  <Button type="submit" size="sm" loading={submittingFeedback} className="w-full">
                    Submit Final Feedback
                  </Button>
                </form>
              )}
            </div>
          )}

          {/* Audit Timeline */}
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm space-y-4">
            <h2 className="font-semibold text-slate-900 text-sm">Audit Timeline</h2>
            <ol className="space-y-4 border-l-2 border-slate-200 pl-4">
              {history.map((h) => (
                <li key={h.id} className="relative text-xs">
                  <span className="absolute -left-[21px] top-1 h-2.5 w-2.5 rounded-full bg-brand-600 ring-4 ring-white" />
                  <div className="font-semibold text-slate-900">{h.eventType.replace(/_/g, ' ')}</div>
                  {h.oldStatus && h.newStatus && h.oldStatus !== h.newStatus && (
                    <div className="text-slate-600">
                      {h.oldStatus} → <strong>{h.newStatus}</strong>
                    </div>
                  )}
                  {h.reason && <div className="mt-0.5 text-slate-600 italic">“{h.reason}”</div>}
                  <div className="mt-0.5 text-slate-400">
                    {h.actorName} • {formatDateTime(h.createdAt)}
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </div>
      </div>

      {/* Action Modal */}
      <Modal
        open={Boolean(modalType)}
        onClose={() => setModalType(null)}
        title={
          modalType === 'route'
            ? 'Route to Department'
            : modalType === 'assign'
            ? complaint.activeAssignmentId ? 'Reassign Staff' : 'Assign Staff'
            : modalType === 'resolve'
            ? 'Resolve Complaint'
            : modalType === 'reopen'
            ? 'Reopen Complaint'
            : modalType === 'reject'
            ? 'Reject Complaint'
            : 'Cancel Complaint'
        }
      >
        <form onSubmit={handleModalSubmit} className="space-y-4">
          {modalType === 'route' && (
            <div className="space-y-1.5">
              <label className="block text-sm font-medium text-slate-700">Target Department</label>
              <select
                required
                value={selectedDeptId}
                onChange={(e) => setSelectedDeptId(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              >
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>
          )}

          {modalType === 'assign' && (
            <div className="space-y-1.5">
              <label className="block text-sm font-medium text-slate-700">Department Employee</label>
              <select
                required
                value={selectedEmployeeId}
                onChange={(e) => setSelectedEmployeeId(e.target.value)}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm"
              >
                {deptMembers.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.userName} ({m.memberType})
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="space-y-1.5">
            <label className="block text-sm font-medium text-slate-700">
              {modalType === 'resolve' ? 'Resolution Notes' : 'Reason / Notes'}
            </label>
            <textarea
              rows={3}
              required={
                modalType === 'resolve' ||
                modalType === 'reopen' ||
                modalType === 'reject' ||
                modalType === 'cancel' ||
                (modalType === 'assign' && Boolean(complaint.activeAssignmentId))
              }
              value={modalInput}
              onChange={(e) => setModalInput(e.target.value)}
              placeholder="Enter details..."
              className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm"
            />
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setModalType(null)}>
              Cancel
            </Button>
            <Button type="submit" loading={modalLoading}>
              Confirm
            </Button>
          </div>
        </form>
      </Modal>
    </PageShell>
  );
}

