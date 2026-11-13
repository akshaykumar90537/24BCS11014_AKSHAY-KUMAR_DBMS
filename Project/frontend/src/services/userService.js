import apiClient from './apiClient';

const userService = {
  async list(params = {}) {
    const res = await apiClient.get('/users', { params });
    return res.data.data;
  },

  async create(payload) {
    const res = await apiClient.post('/users', payload);
    return res.data.data;
  },

  async updateRole(id, role) {
    const res = await apiClient.put(`/users/${id}/role`, { role });
    return res.data.data;
  },

  async updateStatus(id, active) {
    const res = await apiClient.patch(`/users/${id}/status`, { active });
    return res.data.data;
  },
};

export default userService;

