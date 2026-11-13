import apiClient from './apiClient';

const notificationService = {
  async list(page = 0, size = 20) {
    const res = await apiClient.get('/notifications', { params: { page, size } });
    return res.data.data;
  },

  async markRead(id) {
    const res = await apiClient.patch(`/notifications/${id}/read`);
    return res.data.data;
  },

  async markAllRead() {
    const res = await apiClient.patch('/notifications/read-all');
    return res.data.data;
  },
};

export default notificationService;

