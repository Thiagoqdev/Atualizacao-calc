import apiClient from './apiClient';

export const calculoApi = {
  preview: async (dados) => {
    const response = await apiClient.post('/calculos/preview', dados);
    return response.data;
  },

  listar: async (params = {}) => {
    const response = await apiClient.get('/calculos', { params });
    return response.data;
  },

  listarPorProcesso: async (processoId, params = {}) => {
    const response = await apiClient.get(`/processos/${processoId}/calculos`, { params });
    return response.data;
  },

  buscarPorId: async (id) => {
    const response = await apiClient.get(`/calculos/${id}`);
    return response.data;
  },

  criar: async (processoId, dados) => {
    const response = await apiClient.post(`/processos/${processoId}/calculos`, dados);
    return response.data;
  },

  executar: async (id) => {
    const response = await apiClient.post(`/calculos/${id}/executar`);
    return response.data;
  },

  excluir: async (id) => {
    await apiClient.delete(`/calculos/${id}`);
  },

  downloadRelatorio: async (id, formato = 'pdf', nivel = 'completo') => {
    const response = await apiClient.get(`/calculos/${id}/relatorio`, {
      params: { formato, nivel },
      responseType: 'blob',
    });
    return response.data;
  },
};

export default calculoApi;
