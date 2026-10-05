import axiosClient from './axiosClient';

export const getPurchases = () => axiosClient.get('/purchases');
export const getPurchaseById = (id) => axiosClient.get(`/purchases/${id}`);
export const createPurchase = (data) => axiosClient.post('/purchases', data);
export const recordPurchasePayment = (id, amount, paymentMethod, notes) => axiosClient.post(`/purchases/${id}/pay`, null, { params: { amount, paymentMethod, notes: notes || '' } });
