import axiosClient from './axiosClient';

const isMock = import.meta.env.VITE_USE_MOCK_API === 'true';

let mockInvoices = [];

export const getInvoices = async () => {
  if (isMock) {
    return new Promise(resolve => setTimeout(() => resolve(mockInvoices), 500));
  }
  const response = await axiosClient.get('/invoices');
  return response.data;
};

export const getInvoiceById = async (id) => {
  if (isMock) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const invoice = mockInvoices.find(i => i.id === parseInt(id));
        if (invoice) resolve(invoice);
        else reject(new Error('Not found'));
      }, 500);
    });
  }
  const response = await axiosClient.get(`/invoices/${id}`);
  return response.data;
};

export const createInvoice = async (invoiceData) => {
  if (isMock) {
    return new Promise(resolve => {
      setTimeout(() => {
        const newInvoice = { id: Date.now(), ...invoiceData, status: 'PENDING', createdAt: new Date().toISOString() };
        mockInvoices.push(newInvoice);
        resolve(newInvoice);
      }, 500);
    });
  }
  const response = await axiosClient.post('/invoices', invoiceData);
  return response.data;
};

export const cancelInvoice = async (id) => {
  if (isMock) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockInvoices.findIndex(i => i.id === parseInt(id));
        if (index !== -1) {
          mockInvoices[index].status = 'CANCELLED';
          resolve(mockInvoices[index]);
        } else {
          reject(new Error('Not found'));
        }
      }, 500);
    });
  }
  const response = await axiosClient.put(`/invoices/${id}/cancel`);
  return response.data;
};
