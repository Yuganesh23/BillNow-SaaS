import axiosClient from './axiosClient';

export const createPayment = async (paymentData) => {
  const response = await axiosClient.post('/payments', paymentData);
  return response.data;
};

export const getPaymentByInvoice = async (invoiceId) => {
  const response = await axiosClient.get(`/payments/invoice/` + invoiceId);
  return response.data;
};
