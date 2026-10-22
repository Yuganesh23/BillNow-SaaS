import api from './axiosClient';

export const workflowApi = {
  listQuotations: () => api.get('/quotations'),
  createQuotation: (payload) => api.post('/quotations', payload),
  convertQuotation: (id) => api.post(`/quotations/${id}/convert`),
  aging: () => api.get('/customer-ledger/aging'),
  createRecurring: (payload) => api.post('/recurring-invoices', payload),
};
