import apiClient from './apiClient';

const authService = {
  async register(payload) {
    const res = await apiClient.post('/auth/register', payload);
    return res.data.data;
  },

  async login(payload) {
    const res = await apiClient.post('/auth/login', payload);
    return res.data.data;
  },

  async logout(refreshToken) {
    await apiClient.post('/auth/logout', { refreshToken });
  },

  async me() {
    const res = await apiClient.get('/auth/me');
    return res.data.data;
  },

  async updateProfile(payload) {
    const res = await apiClient.put('/auth/me', payload);
    return res.data.data;
  },

  async forgotPassword(email) {
    const res = await apiClient.post('/auth/forgot-password', { email });
    return res.data;
  },

  async resetPassword(payload) {
    const res = await apiClient.post('/auth/reset-password', payload);
    return res.data;
  },
};

export default authService;

