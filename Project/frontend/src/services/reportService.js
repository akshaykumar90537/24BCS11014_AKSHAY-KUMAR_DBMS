import apiClient from './apiClient';

const reportService = {
  async getAdminDashboard() {
    const res = await apiClient.get('/admin/dashboard');
    return res.data.data;
  },

  async getSummary(params = {}) {
    const res = await apiClient.get('/reports/summary', { params });
    return res.data.data;
  },

  async getByDepartment(params = {}) {
    const res = await apiClient.get('/reports/by-department', { params });
    return res.data.data;
  },

  async getTrends(params = {}) {
    const res = await apiClient.get('/reports/trends', { params });
    return res.data.data;
  },

  async getResolutionTime(params = {}) {
    const res = await apiClient.get('/reports/resolution-time', { params });
    return res.data.data;
  },

  async exportCsv(params = {}) {
    const res = await apiClient.get('/reports/export', { params, responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'resolveit-report.csv');
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async listSlaPolicies() {
    const res = await apiClient.get('/admin/sla-policies');
    return res.data.data;
  },

  async updateSlaPolicy(priority, payload) {
    const res = await apiClient.put(`/admin/sla-policies/${priority}`, payload);
    return res.data.data;
  },
};

export default reportService;

