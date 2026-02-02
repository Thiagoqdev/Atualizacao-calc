import apiClient from './apiClient';

export const processoApi = {
  listar: async (params = {}) => {
    const response = await apiClient.get('/processos', { params });
    return response.data;
  },

  buscarPorId: async (id) => {
    const response = await apiClient.get(`/processos/${id}`);
    return response.data;
  },

  criar: async (dados) => {
    const response = await apiClient.post('/processos', dados);
    return response.data;
  },

  atualizar: async (id, dados) => {
    const response = await apiClient.put(`/processos/${id}`, dados);
    return response.data;
  },

  excluir: async (id) => {
    await apiClient.delete(`/processos/${id}`);
  },
};

export default processoApi;
