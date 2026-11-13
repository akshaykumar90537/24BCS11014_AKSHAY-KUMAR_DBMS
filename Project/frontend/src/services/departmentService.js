import apiClient from './apiClient';

const departmentService = {
  async list(includeInactive = false) {
    const res = await apiClient.get('/departments', { params: { includeInactive } });
    return res.data.data;
  },

  async create(payload) {
    const res = await apiClient.post('/departments', payload);
    return res.data.data;
  },

  async update(id, payload) {
    const res = await apiClient.put(`/departments/${id}`, payload);
    return res.data.data;
  },

  async listMembers(departmentId) {
    const res = await apiClient.get(`/departments/${departmentId}/members`);
    return res.data.data;
  },

  async addMember(departmentId, payload) {
    const res = await apiClient.post(`/departments/${departmentId}/members`, payload);
    return res.data.data;
  },

  async removeMember(departmentId, userId) {
    await apiClient.delete(`/departments/${departmentId}/members/${userId}`);
  },

  async listCategories(includeInactive = false) {
    const res = await apiClient.get('/categories', { params: { includeInactive } });
    return res.data.data;
  },

  async createCategory(payload) {
    const res = await apiClient.post('/categories', payload);
    return res.data.data;
  },

  async updateCategory(id, payload) {
    const res = await apiClient.put(`/categories/${id}`, payload);
    return res.data.data;
  },
};

export default departmentService;

