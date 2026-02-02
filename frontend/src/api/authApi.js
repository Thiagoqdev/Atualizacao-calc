import apiClient from './apiClient';

export const authApi = {
  login: async (email, senha) => {
    const response = await apiClient.post('/auth/login', { email, senha });
    return response.data;
  },

  register: async (nomeCompleto, email, senha) => {
    const response = await apiClient.post('/auth/register', {
      nomeCompleto,
      email,
      senha,
    });
    return response.data;
  },

  refresh: async (refreshToken) => {
    const response = await apiClient.post('/auth/refresh', { refreshToken });
    return response.data;
  },

  getMe: async () => {
    const response = await apiClient.get('/auth/me');
    return response.data;
  },
};

export default authApi;
