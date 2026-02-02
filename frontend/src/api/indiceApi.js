import apiClient from './apiClient';

export const indiceApi = {
  listarTabelas: async () => {
    const response = await apiClient.get('/indices');
    return response.data;
  },

  buscarTabelaPorId: async (id) => {
    const response = await apiClient.get(`/indices/${id}`);
    return response.data;
  },

  listarValores: async (tabelaId, de = null, ate = null) => {
    const params = {};
    if (de) params.de = de;
    if (ate) params.ate = ate;

    const response = await apiClient.get(`/indices/${tabelaId}/valores`, { params });
    return response.data;
  },

  importarCSV: async (tabelaId, file) => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post(`/indices/${tabelaId}/valores/import`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  sincronizar: async (tabelaId, dataInicial, dataFinal) => {
    const response = await apiClient.post(`/indices/${tabelaId}/sync`, {
      dataInicial,
      dataFinal,
    });
    return response.data;
  },
};

export default indiceApi;
