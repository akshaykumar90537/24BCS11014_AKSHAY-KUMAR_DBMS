import apiClient from './apiClient';

const complaintService = {
  async list(params = {}) {
    const res = await apiClient.get('/complaints', { params });
    return res.data.data;
  },

  async getById(id) {
    const res = await apiClient.get(`/complaints/${id}`);
    return res.data.data;
  },

  async trackPublic(complaintNumber) {
    const res = await apiClient.get(`/complaints/track/${encodeURIComponent(complaintNumber)}`);
    return res.data.data;
  },

  async potentialDuplicates(categoryId) {
    const res = await apiClient.get('/complaints/potential-duplicates', { params: { categoryId } });
    return res.data.data;
  },

  async create(payload) {
    const res = await apiClient.post('/complaints', payload);
    return res.data.data;
  },

  async update(id, payload) {
    const res = await apiClient.patch(`/complaints/${id}`, payload);
    return res.data.data;
  },

  async cancel(id, reason) {
    const res = await apiClient.post(`/complaints/${id}/cancel`, { reason });
    return res.data.data;
  },

  async close(id, reason) {
    const res = await apiClient.post(`/complaints/${id}/close`, reason ? { reason } : {});
    return res.data.data;
  },

  async reopen(id, reason) {
    const res = await apiClient.post(`/complaints/${id}/reopen`, { reason });
    return res.data.data;
  },

  async reject(id, reason) {
    const res = await apiClient.post(`/complaints/${id}/reject`, { reason });
    return res.data.data;
  },

  async routeToDepartment(id, departmentId, reason) {
    const res = await apiClient.post(`/admin/complaints/${id}/route`, { departmentId, reason });
    return res.data.data;
  },

  async assignEmployee(id, employeeId, reason) {
    const res = await apiClient.post(`/complaints/${id}/assignments`, { employeeId, reason });
    return res.data.data;
  },

  async reassignEmployee(id, assignmentId, employeeId, reason) {
    const res = await apiClient.post(`/complaints/${id}/assignments/${assignmentId}/reassign`, {
      employeeId,
      reason,
    });
    return res.data.data;
  },

  async getAssignments(id) {
    const res = await apiClient.get(`/complaints/${id}/assignments`);
    return res.data.data;
  },

  async startWork(id) {
    const res = await apiClient.post(`/complaints/${id}/start`);
    return res.data.data;
  },

  async resolve(id, resolutionNotes) {
    const res = await apiClient.post(`/complaints/${id}/resolve`, { resolutionNotes });
    return res.data.data;
  },

  async getHistory(id) {
    const res = await apiClient.get(`/complaints/${id}/history`);
    return res.data.data;
  },

  async getComments(id) {
    const res = await apiClient.get(`/complaints/${id}/comments`);
    return res.data.data;
  },

  async addComment(id, body, visibility = 'PUBLIC') {
    const res = await apiClient.post(`/complaints/${id}/comments`, { body, visibility });
    return res.data.data;
  },

  async getAttachments(id) {
    const res = await apiClient.get(`/complaints/${id}/attachments`);
    return res.data.data;
  },

  async uploadAttachment(id, file) {
    const formData = new FormData();
    formData.append('file', file);
    const res = await apiClient.post(`/complaints/${id}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },

  async downloadAttachment(attachmentId, filename) {
    const res = await apiClient.get(`/attachments/${attachmentId}/download`, {
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename || 'attachment');
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async getFeedback(id) {
    const res = await apiClient.get(`/complaints/${id}/feedback`);
    return res.data.data;
  },

  async submitFeedback(id, rating, comment) {
    const res = await apiClient.post(`/complaints/${id}/feedback`, { rating, comment });
    return res.data.data;
  },

  async getEmployeeQueue(params = {}) {
    const res = await apiClient.get('/employee/complaints', { params });
    return res.data.data;
  },

  async getManagerQueue(params = {}) {
    const res = await apiClient.get('/manager/department-queue', { params });
    return res.data.data;
  },

  async getManagerOverdue(params = {}) {
    const res = await apiClient.get('/manager/overdue', { params });
    return res.data.data;
  },
};

export default complaintService;

