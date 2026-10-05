import axiosClient from './axiosClient';

export const getSuppliers = () => axiosClient.get('/suppliers');
export const getSupplierById = (id) => axiosClient.get(`/suppliers/${id}`);
export const createSupplier = (data) => axiosClient.post('/suppliers', data);
export const updateSupplier = (id, data) => axiosClient.put(`/suppliers/${id}`, data);
export const getSupplierLedger = (id) => axiosClient.get(`/suppliers/${id}/ledger`);
export const recordSupplierPayment = (id, amount, method, notes) => axiosClient.post(`/suppliers/${id}/pay`, null, { params: { amount, paymentMethod: method, notes: notes || '' } });
export const deleteSupplier = (id) => axiosClient.delete(`/suppliers/${id}`);
